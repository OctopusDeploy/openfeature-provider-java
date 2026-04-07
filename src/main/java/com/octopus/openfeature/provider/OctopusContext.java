package com.octopus.openfeature.provider;

import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.FlagNotFoundError;
import dev.openfeature.sdk.exceptions.ParseError;
import org.apache.commons.codec.digest.MurmurHash3;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.stream.Collectors.groupingBy;

class OctopusContext {

    private static final System.Logger logger = System.getLogger(OctopusClient.class.getName());
    private final FeatureToggles featureToggles;

    OctopusContext(FeatureToggles featureToggles) {
        this.featureToggles = featureToggles;
    }

    static OctopusContext empty() {
        return new OctopusContext(new FeatureToggles(List.of(), new byte[0]));
    }

    byte[] getContentHash() {
        return featureToggles.getContentHash();
    }

    ProviderEvaluation<Boolean> evaluate(String slug, Boolean defaultValue, EvaluationContext evaluationContext) {
        var toggleValue = featureToggles.getEvaluations().stream()
                .filter(f -> f.getSlug().equalsIgnoreCase(slug))
                .findFirst().orElse(null);

        if (toggleValue == null) {
            throw new FlagNotFoundError();
        }

        if (missingRequiredPropertiesForClientSideEvaluation(toggleValue)) {
            throw new ParseError("Feature toggle " + toggleValue.getSlug() + " is missing necessary information for client-side evaluation.");
        }

        if (!toggleValue.isEnabled()) {
            return ProviderEvaluation.<Boolean>builder()
                    .value(false)
                    .reason(Reason.DEFAULT.toString())
                    .build();
        }

        // EvaluationKey and ClientRolloutPercentage are guaranteed non-null here via missingRequiredPropertiesForClientSideEvaluation()
        String evaluationKey = toggleValue.getEvaluationKey().orElseThrow();
        int rolloutPercentage = toggleValue.getClientRolloutPercentage().orElseThrow();
        String targetingKey = evaluationContext != null ? evaluationContext.getTargetingKey() : null;

        if (targetingKey == null || targetingKey.isEmpty()) {
            if (rolloutPercentage < 100) {
                return ProviderEvaluation.<Boolean>builder()
                        .value(false)
                        .reason(Reason.TARGETING_MATCH.toString())
                        .build();
            }
            // rolloutPercentage == 100: fall through to segment check
        } else {
            if (getNormalizedNumber(evaluationKey, targetingKey) > rolloutPercentage) {
                return ProviderEvaluation.<Boolean>builder()
                        .value(false)
                        .reason(Reason.TARGETING_MATCH.toString())
                        .build();
            }
        }

        if (!toggleValue.hasSegments()) {
            return ProviderEvaluation.<Boolean>builder()
                    .value(true)
                    .reason(Reason.DEFAULT.toString())
                    .build();
        }

        var segments = toggleValue.getSegments().orElseThrow();

        return ProviderEvaluation.<Boolean>builder()
                .value(matchesSegment(evaluationContext, segments))
                .reason(Reason.TARGETING_MATCH.toString())
                .build();
    }

    private boolean missingRequiredPropertiesForClientSideEvaluation(FeatureToggleEvaluation evaluation) {
        if (!evaluation.isEnabled()) {
            return false;
        }

        return evaluation.getClientRolloutPercentage().isEmpty()
                || evaluation.getEvaluationKey().isEmpty()
                || evaluation.getSegments().isEmpty();
    }

    static int getNormalizedNumber(String evaluationKey, String targetingKey) {
        byte[] bytes = (evaluationKey + ":" + targetingKey).getBytes(StandardCharsets.UTF_8);

        // MurmurHash3 32-bit, seed 0. hash32x86 processes tail bytes in little-endian order,
        // matching the reference C spec and equivalent to .NET's MurmurHash.Create32() +
        // BinaryPrimitives.ReadUInt32LittleEndian().
        int hash = MurmurHash3.hash32x86(bytes, 0, bytes.length, 0);

        // Java has no unsigned integer type. Integer.toUnsignedLong() reinterprets the signed
        // int as an unsigned 32-bit value (widened to long) — equivalent to casting to uint in C#.
        long unsignedHash = Integer.toUnsignedLong(hash);

        return (int) (unsignedHash % 100) + 1;
    }

    static boolean matchesSegment(EvaluationContext evaluationContext, List<Segment> segments) {
        if (evaluationContext == null) {
            return false;
        }

        var contextEntries = evaluationContext.asMap();
        var groupedByKey = segments.stream().collect(groupingBy(Segment::getKey));
        return groupedByKey.keySet().stream().allMatch(k -> {
            var values = groupedByKey.get(k);

            return contextEntries.keySet().stream().anyMatch(
                    c -> c.equalsIgnoreCase(k) && values.stream().anyMatch(
                            v -> v.getValue().equalsIgnoreCase(contextEntries.get(c).asString())));

        });
    }

}
