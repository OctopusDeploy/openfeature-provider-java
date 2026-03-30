package com.octopus.openfeature.provider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

class FeatureToggleEvaluation {
    private final String slug;
    private final boolean isEnabled;
    private final String evaluationKey;
    private final List<Segment> segments;
    private final Integer clientRolloutPercentage;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    FeatureToggleEvaluation(
            @JsonProperty(value = "slug", required = true) String slug,
            @JsonProperty(value = "isEnabled", required = true) boolean isEnabled,
            @JsonProperty("evaluationKey") String evaluationKey,
            @JsonProperty("segments") List<Segment> segments,
            @JsonProperty("clientRolloutPercentage") Integer clientRolloutPercentage
    ) {
        this.slug = slug;
        this.isEnabled = isEnabled;

        this.evaluationKey = evaluationKey;
        this.segments = segments == null ? null : List.copyOf(segments);
        this.clientRolloutPercentage = clientRolloutPercentage;
    }

    public String getSlug() {
        return slug;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Optional<String> getEvaluationKey() {
        return Optional.ofNullable(evaluationKey);
    }

    public Optional<List<Segment>> getSegments() {
        return Optional.ofNullable(segments);
    }

    public boolean hasSegments() {
        return segments != null && !segments.isEmpty();
    }

    public Optional<Integer> getClientRolloutPercentage() {
        return Optional.ofNullable(clientRolloutPercentage);
    }
}
