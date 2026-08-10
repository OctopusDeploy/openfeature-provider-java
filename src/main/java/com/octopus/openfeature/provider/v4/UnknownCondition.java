package com.octopus.openfeature.provider.v4;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

/**
 * A client-side condition whose {@code type} discriminator this version of the provider does not
 * recognise, or which carried no discriminator at all. Rather than failing the whole evaluation
 * response, an unrecognised condition is preserved as this type. It always evaluates to
 * {@code false}, so a rule containing an unknown condition can never match — a newer server
 * capability is safely treated as "not met" by an older client.
 *
 * <p>The raw payload of an unknown condition is not retained, only its discriminator.
 */
final class UnknownCondition extends ClientSideCondition {
    private final String type;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    UnknownCondition(
            @JsonProperty("type") String type
    ) {
        this.type = type;
    }

    /**
     * The unrecognised discriminator value, or empty if none was present.
     */
    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }
}
