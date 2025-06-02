package com.octopus.openfeature.provider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class FeatureToggleEvaluation {
    private final String name;
    private final String slug;
    private final boolean isEnabled;
    private final List<Map.Entry<String, String>> segments;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    FeatureToggleEvaluation(@JsonProperty("name") String name, @JsonProperty("slug")String slug, @JsonProperty("isEnabled") boolean isEnabled,
                            @JsonProperty("segments") List<Map.Entry<String, String>> segments) {
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

    public List<Map.Entry<String, String>> getSegments() {
        return segments;
    }
}
