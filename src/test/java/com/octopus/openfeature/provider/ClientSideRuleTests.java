package com.octopus.openfeature.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
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

class ClientSideRuleTests {

    private final ObjectMapper objectMapper = OctopusObjectMapper.INSTANCE;

    private static ClientSideRule rule(ClientSideCondition... conditions) {
        return new ClientSideRule("Rule 1", Arrays.asList(conditions));
    }

    @Test
    void aSingleMatchingConditionMatches() {
        assertThat(rule(new ContextAttributeIsOneOfCondition("plan", List.of("pro")))
                .matches(Contexts.forRules(null, "plan", "pro"))).isTrue();
    }

    @Test
    void conditionsAreCombinedWithAnd() {
        var rule = rule(
                new PercentageByContextCondition(100),
                new ContextAttributeIsOneOfCondition("plan", List.of("pro")));

        assertThat(rule.matches(Contexts.forRules(Contexts.TARGETING_KEY, "plan", "pro")))
                .as("both conditions match").isTrue();
        assertThat(rule.matches(Contexts.forRules(Contexts.TARGETING_KEY, "plan", "free")))
                .as("one condition fails").isFalse();
    }

    @Test
    void aMalformedConditionBehindAFailingOneIsNeverRead() {
        // Conditions stop at the first that does not match, so the rest are never read.
        var rule = rule(
                new ContextAttributeIsOneOfCondition("plan", List.of("pro")),
                new PercentageByContextCondition(null));

        assertThat(rule.matches(Contexts.forRules(Contexts.TARGETING_KEY, "plan", "free"))).isFalse();
    }

    @Test
    void aNamedRuleWithConditionsEvaluates() {
        assertThat(rule(new ContextAttributeIsOneOfCondition("plan", List.of("pro")))
                .matches(Contexts.forRules(null, "plan", "pro"))).isTrue();
        assertThat(rule(new UnknownCondition("some-future-condition"))
                .matches(Contexts.forRules(Contexts.TARGETING_KEY)))
                .as("a condition from a newer server is well-formed, it just never matches").isFalse();
    }

    // Deserialized rather than constructed: the server only defers a named rule carrying at least one
    // condition, so these shapes only arrive off the wire.
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("malformedRules")
    void aMalformedRuleThrowsAParseErrorDescribingTheProblem(String ruleJson, String expectedProblem) throws Exception {
        var rule = objectMapper.readValue(Contexts.json(ruleJson), ClientSideRule.class);

        assertThatThrownBy(() -> rule.matches(Contexts.forRules(Contexts.TARGETING_KEY)))
                .isInstanceOf(ParseError.class)
                .hasMessage(expectedProblem);
    }

    static Stream<Arguments> malformedRules() {
        return Stream.of(
                Arguments.of("{ 'conditions': [ { 'type': 'percentage-by-context', 'percentage': 50 } ] }",
                        "A rule has no name."),
                Arguments.of("{ 'name': 'R', 'conditions': [] }", "Rule 'R' has no conditions."),
                Arguments.of("{ 'name': 'R' }", "Rule 'R' has no conditions."),
                Arguments.of("{ 'name': 'R', 'conditions': null }", "Rule 'R' has no conditions."),
                Arguments.of("{ 'name': 'R', 'conditions': [ null ] }", "Rule 'R' has a missing condition.")
        );
    }
}
