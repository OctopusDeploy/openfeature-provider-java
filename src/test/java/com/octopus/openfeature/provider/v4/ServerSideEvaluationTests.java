package com.octopus.openfeature.provider.v4;

import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.exceptions.ParseError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ServerSideEvaluation#evaluate} against a well-formed response. Rules and conditions have
 * their own suites; malformed responses are covered by {@link MalformedEvaluationTests}.
 */
class ServerSideEvaluationTests {

    private static ServerSideEvaluation serverResolved(boolean value, String reason) {
        return new ServerSideEvaluation(Contexts.SLUG, value, reason, null, null);
    }

    private static ServerSideEvaluation deferred(ClientSideRule... rules) {
        return new ServerSideEvaluation(Contexts.SLUG, null, null, Contexts.EVALUATION_KEY, List.of(rules));
    }

    private static ClientSideRule ruleMatching(String name, String plan) {
        return new ClientSideRule(name, List.of(new ContextAttributeIsOneOfCondition("plan", List.of(plan))));
    }

    @ParameterizedTest(name = "[{index}] value={0}")
    @ValueSource(booleans = {true, false})
    void serverResolvedFlagReturnsTheServerValueAndReason(boolean value) {
        var result = serverResolved(value, "the server said so").evaluate(Contexts.openFeature(null));

        assertThat(result.getValue()).isEqualTo(value);
        assertThat(result.getReason()).isEqualTo("the server said so");
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void serverResolvedFlagWithNoReasonThrowsAParseError() {
        assertThatThrownBy(() -> serverResolved(true, null).evaluate(Contexts.openFeature(null)))
                .isInstanceOf(ParseError.class)
                .hasMessage("The flag has a value but has no reason.")
                .extracting(thrown -> ((ParseError) thrown).getErrorCode()).isEqualTo(ErrorCode.PARSE_ERROR);
    }

    @Test
    void matchingRuleResolvesToTrueWithTheMatchedRuleReason() {
        var flag = deferred(ruleMatching("beta-testers", "beta"));

        var result = flag.evaluate(Contexts.openFeature(null, "plan", "beta"));

        assertThat(result.getValue()).isTrue();
        assertThat(result.getReason()).isEqualTo("Matched rule 'beta-testers'.");
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void noMatchingRuleResolvesToFalseWithTheDidNotMatchReason() {
        // Off, not defaulted: this resolves rather than erroring, so no default value is involved.
        var flag = deferred(ruleMatching("beta-testers", "beta"));

        var result = flag.evaluate(Contexts.openFeature(null, "plan", "free"));

        assertThat(result.getValue()).isFalse();
        assertThat(result.getReason()).isEqualTo("Did not match any rules.");
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void rulesAcrossAFlagAreCombinedWithOr() {
        var flag = deferred(
                ruleMatching("beta-testers", "beta"),
                new ClientSideRule("internal",
                        List.of(new ContextAttributeIsOneOfCondition("email", List.of("staff@octopus.com")))));

        // Both rules match, so the reason names the first one that did.
        var both = flag.evaluate(
                Contexts.openFeature(null, "plan", "beta", "email", "staff@octopus.com"));
        assertThat(both.getValue()).isTrue();
        assertThat(both.getReason()).isEqualTo("Matched rule 'beta-testers'.");

        var second = flag.evaluate(Contexts.openFeature(null, "email", "staff@octopus.com"));
        assertThat(second.getValue()).as("second rule matches").isTrue();
        assertThat(second.getReason()).isEqualTo("Matched rule 'internal'.");

        var neither = flag.evaluate(Contexts.openFeature(null, "plan", "free"));
        assertThat(neither.getValue()).as("no rule matches").isFalse();
        assertThat(neither.getReason()).isEqualTo("Did not match any rules.");
    }

    @Test
    void aNullContextIsTreatedAsAnEmptyContext() {
        assertThat(deferred(ruleMatching("pro-users", "pro")).evaluate(null).getValue())
                .as("there is no attribute to match").isFalse();
        assertThat(deferred(new ClientSideRule("everyone", List.of(new PercentageByContextCondition(100))))
                .evaluate(null).getValue())
                .as("a 100%% rollout matches without a targeting key").isTrue();
    }
}
