package com.octopus.openfeature.provider;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Segment {
    private final String key;
    private final String value;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Segment(
            @JsonProperty("key") String key,
            @JsonProperty("value") String value
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
