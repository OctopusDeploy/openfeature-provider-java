package com.octopus.openfeature.provider.v4;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class PercentageRolloutTests {

    // The same vectors the v3 path is held to. v3 keeps its own copy of this hash — package access is
    // not hierarchical, so neither can call the other while both stay package-private — and running one
    // list against both is what stops the two drifting apart.
    @ParameterizedTest(name = "[{index}] ({0}, {1}) -> {2}")
    @MethodSource("com.octopus.openfeature.provider.RolloutVectors#cases")
    void getNormalizedNumberMatchesExpectedValue(String evaluationKey, String targetingKey, int expected) {
        assertThat(PercentageRollout.getNormalizedNumber(evaluationKey, targetingKey)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] ({0}, {1}) -> {2}")
    @MethodSource("com.octopus.openfeature.provider.RolloutVectors#cases")
    void includesEveryPercentageFromTheBucketUpwards(String evaluationKey, String targetingKey, int bucket) {
        assertThat(PercentageRollout.includes(evaluationKey, targetingKey, bucket))
                .as("the bucket itself is inside the rollout").isTrue();
        assertThat(PercentageRollout.includes(evaluationKey, targetingKey, bucket - 1))
                .as("one percent below the bucket is outside it").isFalse();
        assertThat(PercentageRollout.includes(evaluationKey, targetingKey, 100))
                .as("a full rollout includes every bucket").isTrue();
    }

    @Test
    void nothingIsIncludedAtZeroPercent() {
        // The lowest bucket is 1, so a 0% rollout can never include a targeting key.
        assertThat(PercentageRollout.includes(Contexts.EVALUATION_KEY, Contexts.TARGETING_KEY, 0)).isFalse();
    }
}
