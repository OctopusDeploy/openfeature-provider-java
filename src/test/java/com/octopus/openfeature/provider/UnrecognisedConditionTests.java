package com.octopus.openfeature.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A condition naming a type this version does not recognise is a capability from a newer server, not
 * a bad payload: it fails its own rule and nothing else. The deliberate departure from
 * {@link MalformedEvaluationTests}, which covers every other shape — including a condition with no
 * type at all.
 */
class UnrecognisedConditionTests {

    private final ObjectMapper objectMapper = OctopusObjectMapper.INSTANCE;

    private ServerSideEvaluation flag(String singleQuotedJson) throws Exception {
        return objectMapper.readValue(Contexts.json(singleQuotedJson), ServerSideEvaluation.class);
    }

    @Test
    void anUnrecognisedConditionTypeFailsItsRuleWithoutAnError() throws Exception {
        var flag = flag("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': ["
                + " { 'name': 'Something newer than this library',"
                + "   'conditions': [ { 'type': 'not-a-real-condition', 'key': 'license', 'values': [ 'trial' ] } ] } ] }");

        var result = flag.evaluate(Contexts.openFeature(Contexts.TARGETING_KEY, "license", "trial"));

        assertThat(result.getValue()).isFalse();
        assertThat(result.getErrorCode()).isNull();
        assertThat(result.getReason()).isEqualTo("Did not match any rules.");
    }

    @Test
    void anUnrecognisedConditionInOneRuleLeavesTheOtherRulesToDecide() throws Exception {
        var flag = flag("{ 'slug': 'my-feature', 'evaluationKey': 'evaluation-key', 'rules': ["
                + " { 'name': 'Something newer than this library',"
                + "   'conditions': [ { 'type': 'not-a-real-condition', 'key': 'license', 'values': [ 'trial' ] } ] },"
                + " { 'name': 'Beta ring',"
                + "   'conditions': [ { 'type': 'context-attribute-is-one-of', 'key': 'ring', 'values': [ 'beta' ] } ] } ] }");

        var result = flag.evaluate(Contexts.openFeature(Contexts.TARGETING_KEY, "license", "trial", "ring", "beta"));

        assertThat(result.getValue()).isTrue();
        assertThat(result.getErrorCode()).isNull();
        assertThat(result.getReason()).isEqualTo("Matched rule 'Beta ring'.");
    }
}
