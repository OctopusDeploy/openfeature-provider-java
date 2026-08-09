package com.octopus.openfeature.provider.v4;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.octopus.openfeature.provider.RolloutBucketing;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.exceptions.ParseError;

import java.util.Optional;

/**
 * Matches when the OpenFeature targeting key falls within the {@code percentage}% rollout.
 */
@JsonIgnoreProperties("type") // The discriminator selects this type; it is not modelled as a property.
@JsonDeserialize(using = JsonDeserializer.None.class) // Resets the base type's deserializer; see ClientSideConditionDeserializer.
final class PercentageByContextCondition extends ClientSideCondition {
    private final Integer percentage;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    PercentageByContextCondition(
            @JsonProperty("percentage") Integer percentage
    ) {
        this.percentage = percentage;
    }

    /**
     * The rollout percentage, 0–100. Boxed so an absent {@code percentage} stays distinguishable from
     * an explicit {@code 0}, which is a legitimate "nobody".
     */
    public Optional<Integer> getPercentage() {
        return Optional.ofNullable(percentage);
    }

    @Override
    boolean matches(ClientSideEvaluationContext context) {
        if (percentage == null) {
            throw new ParseError("A condition is missing a percentage value.");
        }

        // Rejected rather than clamped: reading 101 as "everyone" would turn a flag on off the back of
        // a bad payload.
        if (percentage < 0 || percentage > 100) {
            throw new ParseError("A condition has a percentage of " + percentage + ".");
        }

        EvaluationContext openFeatureContext = context.getOpenFeatureContext();
        String targetingKey = openFeatureContext == null ? null : openFeatureContext.getTargetingKey();

        // Nothing to bucket, so only a full rollout matches — as the server treats an untenanted caller.
        if (targetingKey == null || targetingKey.isEmpty()) {
            return percentage >= 100;
        }

        // Shared with v3 so a rollout lands on the same users across versions and provider libraries.
        return RolloutBucketing.getNormalizedNumber(context.getEvaluationKey(), targetingKey) <= percentage;
    }
}
