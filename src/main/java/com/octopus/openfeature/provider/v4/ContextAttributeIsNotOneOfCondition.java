package com.octopus.openfeature.provider.v4;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * Matches when the context attribute {@code key} is not one of {@code values}. A missing attribute
 * matches.
 */
@JsonIgnoreProperties("type") // The discriminator selects this type; it is not modelled as a property.
@JsonDeserialize(using = JsonDeserializer.None.class) // Resets the base type's deserializer; see ClientSideConditionDeserializer.
final class ContextAttributeIsNotOneOfCondition extends ClientSideCondition {
    private final String key;
    private final List<String> values;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    ContextAttributeIsNotOneOfCondition(
            @JsonProperty("key") String key,
            @JsonProperty("values") List<String> values
    ) {
        this.key = key;
        this.values = WireLists.copyOrNull(values);
    }

    public String getKey() {
        return key;
    }

    public List<String> getValues() {
        return values;
    }

    @Override
    boolean matches(ClientSideEvaluationContext context) {
        return !ContextAttributes.isOneOf(context, key, values);
    }
}
