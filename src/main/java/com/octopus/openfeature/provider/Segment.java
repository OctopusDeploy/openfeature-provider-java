package com.octopus.openfeature.provider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class Segment {
    private final String key;
    private final String value;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Segment(
        @JsonProperty(value = "key", required = true) String key,
        @JsonProperty(value = "value", required = true) String value
    ) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
