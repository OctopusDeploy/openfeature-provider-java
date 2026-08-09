package com.octopus.openfeature.provider.v4;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.openfeature.provider.TestObjectMapper;
import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.exceptions.ParseError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A response the server could not legitimately have sent throws {@link ParseError}. The one
 * exception — a condition type this client does not recognise — is covered by
 * {@link UnrecognisedConditionTests}.
 *
 * <p>Every case is deserialized rather than constructed: these shapes are only reachable off the
 * wire.
 */
class MalformedEvaluationTests {

    private final ObjectMapper objectMapper = TestObjectMapper.INSTANCE;

    /**
     * Satisfies every rule below, so a flag that failed to throw would visibly turn on.
     */
    private static EvaluationContext matchingContext() {
        return Contexts.openFeature(Contexts.TARGETING_KEY, "license", "trial", "ring", "beta");
    }

    private ServerSideEvaluation flag(String singleQuotedJson) throws Exception {
        return objectMapper.readValue(Contexts.json(singleQuotedJson), ServerSideEvaluation.class);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("malformedFlags")
    void aMalformedFlagThrowsAParseError(String flagJson, String expectedProblem) throws Exception {
        var flag = flag(flagJson);

        assertThatThrownBy(() -> flag.evaluate(matchingContext()))
                .isInstanceOf(ParseError.class)
                .hasMessage(expectedProblem)
                .extracting(thrown -> ((ParseError) thrown).getErrorCode()).isEqualTo(ErrorCode.PARSE_ERROR);
    }

    static Stream<Arguments> malformedFlags() {
        return Stream.of(
                // Neither shape, or both at once.
                Arguments.of("{ 'slug': 'my-feature' }",
                        "The flag has neither a value nor rules."),
                Arguments.of("{ 'slug': 'my-feature', 'value': true, 'reason': 'Enabled.', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Beta ring', 'conditions': [ { 'type': 'context-attribute-is-one-of', 'key': 'ring', 'values': [ 'beta' ] } ] } ] }",
                        "The flag has both a server-resolved value and client-side rules."),
                // Deferred, but not evaluable.
                Arguments.of("{ 'slug': 'my-feature', 'rules': [ { 'name': 'Beta ring', 'conditions': [ { 'type': 'context-attribute-is-one-of', 'key': 'ring', 'values': [ 'beta' ] } ] } ] }",
                        "The flag defers to the client but has no evaluation key."),
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [] }",
                        "The flag defers to the client with no rules."),
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ null ] }",
                        "The flag has a missing rule."),
                // Rules.
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'conditions': [ { 'type': 'context-attribute-is-one-of', 'key': 'ring', 'values': [ 'beta' ] } ] } ] }",
                        "A rule has no name."),
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Beta ring', 'conditions': [] } ] }",
                        "Rule 'Beta ring' has no conditions."),
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Beta ring' } ] }",
                        "Rule 'Beta ring' has no conditions."),
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Beta ring', 'conditions': null } ] }",
                        "Rule 'Beta ring' has no conditions."),
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Beta ring', 'conditions': [ null ] } ] }",
                        "Rule 'Beta ring' has a missing condition."),
                // Conditions with no usable type. Unlike an unrecognised type, no server version emits these.
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Trial licences', 'conditions': [ { 'key': 'license', 'values': [ 'trial' ] } ] } ] }",
                        "A condition is missing a type."),
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Trial licences', 'conditions': [ { 'type': 123, 'key': 'license', 'values': [ 'trial' ] } ] } ] }",
                        "A condition is missing a type."),
                // percentage-by-context.
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Partial rollout', 'conditions': [ { 'type': 'percentage-by-context' } ] } ] }",
                        "A condition is missing a percentage value."),
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Partial rollout', 'conditions': [ { 'type': 'percentage-by-context', 'percentage': 101 } ] } ] }",
                        "A condition has a percentage of 101."),
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Partial rollout', 'conditions': [ { 'type': 'percentage-by-context', 'percentage': -1 } ] } ] }",
                        "A condition has a percentage of -1."),
                // Attribute conditions.
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Trial licences', 'conditions': [ { 'type': 'context-attribute-is-one-of', 'key': 'license' } ] } ] }",
                        "A condition is missing values."),
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Trial licences', 'conditions': [ { 'type': 'context-attribute-is-one-of', 'key': 'license', 'values': [] } ] } ] }",
                        "A condition is missing values."),
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Trial licences', 'conditions': [ { 'type': 'context-attribute-is-one-of', 'key': 'license', 'values': [ null ] } ] } ] }",
                        "A condition is missing a value."),
                Arguments.of("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': [ { 'name': 'Trial licences', 'conditions': [ { 'type': 'context-attribute-is-not-one-of', 'values': [ 'trial' ] } ] } ] }",
                        "A condition is missing a key.")
        );
    }

    @Test
    void aMalformedRuleFailsTheFlagEvenWhenALaterRuleMatches() throws Exception {
        // The second rule matches, but the first is read before it: a rule the client cannot make sense
        // of is not skipped in favour of the ones that happened to parse.
        var flag = flag("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': ["
                + " { 'name': 'Trial licences', 'conditions': [ { 'key': 'license', 'values': [ 'trial' ] } ] },"
                + " { 'name': 'Beta ring', 'conditions': [ { 'type': 'context-attribute-is-one-of', 'key': 'ring', 'values': [ 'beta' ] } ] } ] }");

        assertThatThrownBy(() -> flag.evaluate(matchingContext()))
                .isInstanceOf(ParseError.class)
                .hasMessage("A condition is missing a type.");
    }

    @Test
    void aMalformedRuleBehindAMatchingRuleIsNeverRead() throws Exception {
        // Nothing checks the response up front, so a rule only fails the flag if evaluation reaches it.
        var flag = flag("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': ["
                + " { 'name': 'Beta ring', 'conditions': [ { 'type': 'context-attribute-is-one-of', 'key': 'ring', 'values': [ 'beta' ] } ] },"
                + " { 'name': 'Trial licences', 'conditions': [ { 'key': 'license', 'values': [ 'trial' ] } ] } ] }");

        var result = flag.evaluate(matchingContext());

        assertThat(result.getValue()).isTrue();
        assertThat(result.getReason()).isEqualTo("Matched rule 'Beta ring'.");
    }

    @Test
    void aMalformedFlagDoesNotAffectTheRestOfTheResponse() throws Exception {
        List<ServerSideEvaluation> flags = objectMapper.readValue(
                Contexts.json("[ { 'slug': 'malformed-feature' },"
                        + " { 'slug': 'well-formed-feature', 'value': true, 'reason': 'The flag is enabled for this environment.' } ]"),
                new TypeReference<List<ServerSideEvaluation>>() {});

        assertThatThrownBy(() -> flags.get(0).evaluate(matchingContext())).isInstanceOf(ParseError.class);

        var wellFormed = flags.get(1).evaluate(matchingContext());
        assertThat(wellFormed.getValue()).isTrue();
        assertThat(wellFormed.getErrorCode()).isNull();
        assertThat(wellFormed.getReason()).isEqualTo("The flag is enabled for this environment.");
    }
}
