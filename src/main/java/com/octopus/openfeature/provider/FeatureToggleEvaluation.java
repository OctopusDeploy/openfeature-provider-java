package com.octopus.openfeature.provider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

class FeatureToggleEvaluation {
    private final String name;
    private final String slug;
    private final boolean isEnabled;
    private final List<Segment> segments;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    FeatureToggleEvaluation(
        @JsonProperty("name") String name,
        @JsonProperty("slug") String slug,
        @JsonProperty("isEnabled") boolean isEnabled,
        @JsonProperty("segments") List<Segment> segments
    ) {
        this.name = name;
        this.slug = slug;
        this.isEnabled = isEnabled;

        this.segments = new ArrayList<>();
        if (segments != null) {
            this.segments.addAll(segments);
        }
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public List<Segment> getSegments() {
        return Collections.unmodifiableList(segments);
    }
}
