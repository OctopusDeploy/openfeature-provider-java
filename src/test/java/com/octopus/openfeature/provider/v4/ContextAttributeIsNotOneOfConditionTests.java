package com.octopus.openfeature.provider.v4;

import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.exceptions.ParseError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextAttributeIsNotOneOfConditionTests {

    @Test
    void matchesUnlessTheAttributeValueIsListed() {
        var condition = new ContextAttributeIsNotOneOfCondition("region", List.of("eu"));

        assertThat(condition.matches(Contexts.forRules(null, "region", "us"))).isTrue();
        assertThat(condition.matches(Contexts.forRules(null, "region", "eu"))).isFalse();
        assertThat(condition.matches(Contexts.forRules(null)))
                .as("a missing attribute is not one of the values").isTrue();
    }

    @Test
    void theKeyAndValueAreCaseInsensitive() {
        var condition = new ContextAttributeIsNotOneOfCondition("Region", List.of("EU"));

        assertThat(condition.matches(Contexts.forRules(null, "region", "eu"))).isFalse();
    }

    @Test
    void aNonStringValueIsTreatedAsAbsent() {
        // Absent means "not one of", so the condition matches.
        var context = new ClientSideEvaluationContext(
                Contexts.EVALUATION_KEY, new MutableContext().add("user-id", 1234));

        assertThat(new ContextAttributeIsNotOneOfCondition("user-id", List.of("1234")).matches(context)).isTrue();
    }

    @Test
    void aNullOpenFeatureContextMatches() {
        assertThat(new ContextAttributeIsNotOneOfCondition("region", List.of("eu"))
                .matches(Contexts.withoutOpenFeatureContext())).isTrue();
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("missingKeyOrValues")
    void aMissingKeyOrValuesThrowsAParseError(String key, List<String> values, String expectedProblem) {
        assertThatThrownBy(() -> new ContextAttributeIsNotOneOfCondition(key, values)
                .matches(Contexts.forRules(null, "region", "us")))
                .isInstanceOf(ParseError.class)
                .hasMessage(expectedProblem);
    }

    static Stream<Arguments> missingKeyOrValues() {
        return Stream.of(
                Arguments.of(null, List.of("eu"), "A condition is missing a key."),
                Arguments.of("region", null, "A condition is missing values."),
                Arguments.of("region", List.of(), "A condition is missing values.")
        );
    }

    @Test
    void aMissingValueInTheListThrowsAParseError() {
        var values = Arrays.asList("eu", null);

        assertThatThrownBy(() -> new ContextAttributeIsNotOneOfCondition("region", values)
                .matches(Contexts.forRules(null, "region", "us")))
                .isInstanceOf(ParseError.class)
                .hasMessage("A condition is missing a value.");
    }
}
