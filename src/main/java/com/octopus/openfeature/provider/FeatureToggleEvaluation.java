package com.octopus.openfeature.provider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

class FeatureToggleEvaluation {
    private final String slug;
    private final boolean isEnabled;
    private final Optional<String> evaluationKey;
    private final Optional<List<Segment>> segments;
    private final Optional<Integer> clientRolloutPercentage;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    FeatureToggleEvaluation(
            @JsonProperty(value = "slug", required = true) String slug,
            @JsonProperty(value = "isEnabled", required = true) boolean isEnabled,
            @JsonProperty("evaluationKey") Optional<String> evaluationKey,
            @JsonProperty("segments") Optional<List<Segment>> segments,
            @JsonProperty("clientRolloutPercentage") Optional<Integer> clientRolloutPercentage
    ) {
        this.slug = slug;
        this.isEnabled = isEnabled;

        this.evaluationKey = evaluationKey;
        this.segments = segments;
        this.clientRolloutPercentage = clientRolloutPercentage;
    }

    public String getSlug() {
        return slug;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Optional<List<Segment>> getSegments() {
        return segments.map(Collections::unmodifiableList);
    }

    public boolean hasSegments() {
        return segments != null && segments.isPresent() && !segments.get().isEmpty();
    }
}
