package com.octopus.openfeature.provider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.openfeature.sdk.exceptions.ParseError;

import java.util.Optional;

/**
 * A condition whose {@code type} this version of the provider does not recognise. It never matches,
 * so a newer server's capability is treated as "not met" by an older client rather than failing the
 * flag.
 *
 * <p>The raw payload of an unknown condition is not retained, only its discriminator.
 */
@JsonDeserialize(using = JsonDeserializer.None.class) // Resets the base type's deserializer; see ClientSideConditionDeserializer.
final class UnknownCondition extends ClientSideCondition {
    private final String type;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    UnknownCondition(
            @JsonProperty("type") String type
    ) {
        this.type = type;
    }

    /**
     * The unrecognised discriminator value, or empty if none was present or it was not a string.
     */
    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    @Override
    boolean matches(ClientSideEvaluationContext context) {
        // No server version emits a condition without a type, so unlike an unrecognised type this is a
        // response that could not have been sent.
        if (type == null) {
            throw new ParseError("A condition is missing a type.");
        }

        return false;
    }
}
