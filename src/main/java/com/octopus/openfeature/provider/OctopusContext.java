package com.octopus.openfeature.provider;

import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.FlagNotFoundError;

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
        // find the feature toggle matching the slug
        var toggleValue = featureToggles.getEvaluations().stream().filter(f -> f.getSlug().equalsIgnoreCase(slug)).findFirst().orElse(null);

        // this exception will be handled by OpenFeature, and the default value will be used
        if (toggleValue == null) {
           throw new FlagNotFoundError(); 
        }
        
        // if the toggle is disabled, or if it has no segments, then we don't need to evaluate dynamically 
        if (!toggleValue.isEnabled() || toggleValue.getSegments().isEmpty()) {
            return ProviderEvaluation.<Boolean>builder()
                    .value(toggleValue.isEnabled())
                    .reason(Reason.DEFAULT.toString())
                    .build();
        }
        
        // If the toggle is enabled and has segments configured, then we need to evaluate dynamically, 
        // checking the context matches the segments
        return ProviderEvaluation.<Boolean>builder()
                .value(MatchesSegment(evaluationContext, toggleValue.getSegments()))
                .reason(Reason.TARGETING_MATCH.toString())
                .build();
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
