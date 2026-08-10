package com.octopus.openfeature.provider.v4;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Matches when the context attribute {@code key} is one of {@code values}.
 */
@JsonIgnoreProperties("type") // The discriminator is visible to subtypes; this type does not model it.
final class ContextAttributeIsOneOfCondition extends ClientSideCondition {
    private final String key;
    private final List<String> values;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    ContextAttributeIsOneOfCondition(
            @JsonProperty(value = "key", required = true) String key,
            @JsonProperty(value = "values", required = true) List<String> values
    ) {
        this.key = key;
        this.values = List.copyOf(values);
    }

    public String getKey() {
        return key;
    }

    public List<String> getValues() {
        return values;
    }
}
