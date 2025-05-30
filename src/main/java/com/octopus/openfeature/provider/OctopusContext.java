package com.octopus.openfeature.provider;

import dev.openfeature.sdk.*;

import java.util.List;
import java.util.Map;

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
    
    byte[] getContentHash() { return featureToggles.getContentHash(); }

    ProviderEvaluation<Boolean> evaluate(String slug, Boolean defaultValue, EvaluationContext evaluationContext) {
        var toggleValue = featureToggles.getEvaluations().stream().filter(f -> f.getSlug().equals(slug)).findFirst().orElse(null);

        if (toggleValue == null) {
            return ProviderEvaluation.<Boolean>builder()
                    .errorMessage(String.format("flag: %s not found", slug))
                    .errorCode(ErrorCode.FLAG_NOT_FOUND)
                    .build();
        }

        return ProviderEvaluation.<Boolean>builder()
                .value(evaluate(toggleValue, evaluationContext))
                .build();
    }

    private Boolean evaluate(FeatureToggleEvaluation evaluation, EvaluationContext evaluationContext) {
        return evaluation.isEnabled() && (evaluation.getSegments().isEmpty() || MatchesSegment(evaluationContext, evaluation.getSegments()));
    }

    private Boolean MatchesSegment(EvaluationContext evaluationContext, List<Map.Entry<String, String>> segments) {
        if (evaluationContext == null) {
            return false;
        }

        var contextEntries = evaluationContext.asMap();
        var groupedByKey = segments.stream().collect(groupingBy(Map.Entry::getKey));
        return groupedByKey.keySet().stream().allMatch(k -> {
            var values = groupedByKey.get(k);

            return contextEntries.keySet().stream().anyMatch(
                    c -> c.equalsIgnoreCase(k) && values.stream().anyMatch(
                            v -> v.getValue().equalsIgnoreCase(contextEntries.get(c).asString())));

        });
    }

}
