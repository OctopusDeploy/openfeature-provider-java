package com.octopus.openfeature.provider;

import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.exceptions.ParseError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextAttributeIsOneOfConditionTests {

    @Test
    void matchesWhenTheAttributeValueIsListed() {
        var condition = new ContextAttributeIsOneOfCondition("user-id", List.of("1234", "5678"));

        assertThat(condition.matches(Contexts.forRules(null, "user-id", "5678"))).isTrue();
        assertThat(condition.matches(Contexts.forRules(null, "user-id", "9999"))).isFalse();
        assertThat(condition.matches(Contexts.forRules(null)))
                .as("a missing attribute is not one of the values").isFalse();
    }

    @Test
    void theKeyAndValueAreCaseInsensitive() {
        var condition = new ContextAttributeIsOneOfCondition("Region", List.of("EU", "US"));

        assertThat(condition.matches(Contexts.forRules(null, "region", "eu"))).isTrue();
        assertThat(condition.matches(Contexts.forRules(null, "REGION", "Us"))).isTrue();
    }

    @ParameterizedTest(name = "[{index}] {0}={1}, {2}={3}")
    @CsvSource({"Plan,free,plan,pro", "plan,pro,Plan,free"})
    void everyEntryWhoseKeyMatchesIsChecked(String firstKey, String firstValue, String secondKey, String secondValue) {
        // A context can hold several case variants of one key, and the map's iteration order is not
        // guaranteed, so checking only the first matching entry would evaluate inconsistently.
        var condition = new ContextAttributeIsOneOfCondition("plan", List.of("pro"));

        var context = Contexts.forRules(null, firstKey, firstValue, secondKey, secondValue);

        assertThat(condition.matches(context))
                .as("one of the 'plan' entries is 'pro', whichever order they are iterated in").isTrue();
    }

    @Test
    void aNonStringValueIsTreatedAsAbsent() {
        // Value.asString() is null for a non-string, so a numeric attribute never matches a string value.
        var context = new ClientSideEvaluationContext(
                Contexts.EVALUATION_KEY, new MutableContext().add("user-id", 1234));

        assertThat(new ContextAttributeIsOneOfCondition("user-id", List.of("1234")).matches(context)).isFalse();
    }

    @Test
    void aNullOpenFeatureContextDoesNotMatch() {
        assertThat(new ContextAttributeIsOneOfCondition("plan", List.of("pro"))
                .matches(Contexts.withoutOpenFeatureContext())).isFalse();
    }

    // A condition with nothing to match on has no defensible answer, so it fails the evaluation.
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nothingToMatchOn")
    void aConditionWithNothingToMatchOnThrowsAParseError(String key, List<String> values, String expectedProblem) {
        assertThatThrownBy(() -> new ContextAttributeIsOneOfCondition(key, values)
                .matches(Contexts.forRules(null, "plan", "pro")))
                .isInstanceOf(ParseError.class)
                .hasMessage(expectedProblem);
    }

    static Stream<Arguments> nothingToMatchOn() {
        return Stream.of(
                Arguments.of(null, List.of("pro"), "A condition is missing a key."),
                Arguments.of("plan", null, "A condition is missing values."),
                Arguments.of("plan", List.of(), "A condition is missing values."),
                Arguments.of("plan", Arrays.asList("pro", null), "A condition is missing a value.")
        );
    }
}
