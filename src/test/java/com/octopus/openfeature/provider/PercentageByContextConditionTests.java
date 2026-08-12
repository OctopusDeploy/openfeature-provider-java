package com.octopus.openfeature.provider;

import dev.openfeature.sdk.exceptions.ParseError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PercentageByContextConditionTests {

    @Test
    void targetingKeyInsideTheRolloutMatches() {
        assertThat(new PercentageByContextCondition(Contexts.TARGETING_KEY_BUCKET)
                .matches(Contexts.forRules(Contexts.TARGETING_KEY))).isTrue();
    }

    @Test
    void targetingKeyOutsideTheRolloutDoesNotMatch() {
        assertThat(new PercentageByContextCondition(Contexts.TARGETING_KEY_BUCKET - 1)
                .matches(Contexts.forRules(Contexts.TARGETING_KEY))).isFalse();
    }

    @Test
    void withoutATargetingKeyOnlyAFullRolloutMatches() {
        assertThat(new PercentageByContextCondition(100).matches(Contexts.forRules(null)))
                .as("a 100% rollout matches even without a targeting key").isTrue();
        assertThat(new PercentageByContextCondition(99).matches(Contexts.forRules(null)))
                .as("a partial rollout cannot bucket without a targeting key").isFalse();
        assertThat(new PercentageByContextCondition(50).matches(Contexts.forRules("")))
                .as("an empty targeting key is treated the same as none").isFalse();
    }

    @Test
    void atZeroPercentNothingMatches() {
        // The lowest bucket is 1, so nothing is included at 0%.
        assertThat(new PercentageByContextCondition(0)
                .matches(Contexts.forRules(Contexts.TARGETING_KEY))).isFalse();
    }

    @Test
    void withANullOpenFeatureContextOnlyAFullRolloutMatches() {
        var context = Contexts.withoutOpenFeatureContext();

        assertThat(new PercentageByContextCondition(100).matches(context)).isTrue();
        assertThat(new PercentageByContextCondition(99).matches(context)).isFalse();
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @MethodSource("invalidPercentages")
    void anAbsentOrOutOfRangePercentageThrowsAParseError(Integer percentage, String expectedProblem) {
        assertThatThrownBy(() -> new PercentageByContextCondition(percentage)
                .matches(Contexts.forRules(Contexts.TARGETING_KEY)))
                .isInstanceOf(ParseError.class)
                .hasMessage(expectedProblem);
    }

    static Stream<Arguments> invalidPercentages() {
        return Stream.of(
                Arguments.of(null, "A condition is missing a percentage value."),
                Arguments.of(101, "A condition has a percentage of 101."),
                Arguments.of(-1, "A condition has a percentage of -1.")
        );
    }
}
