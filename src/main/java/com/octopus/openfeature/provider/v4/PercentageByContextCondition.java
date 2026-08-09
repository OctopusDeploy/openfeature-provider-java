package com.octopus.openfeature.provider.v4;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Matches when the OpenFeature targeting key falls within the {@code percentage}% rollout.
 */
@JsonIgnoreProperties("type") // The discriminator is visible to subtypes; this type does not model it.
final class PercentageByContextCondition extends ClientSideCondition {
    private final int percentage;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    PercentageByContextCondition(
            @JsonProperty(value = "percentage", required = true) int percentage
    ) {
        this.percentage = percentage;
    }

    public int getPercentage() {
        return percentage;
    }
}
