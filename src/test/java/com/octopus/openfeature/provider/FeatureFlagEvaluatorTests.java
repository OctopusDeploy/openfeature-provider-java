package com.octopus.openfeature.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.exceptions.FlagNotFoundError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Resolving a flag from an evaluation response. The evaluation itself — server-resolved values,
 * client-side rules and malformed responses — is covered by {@link ServerSideEvaluationTests} and the
 * suites around it; these cases cover finding the flag, and what happens when it is not there.
 */
class FeatureFlagEvaluatorTests {

    private static ServerSideEvaluation serverResolved(String slug, boolean value) {
        return new ServerSideEvaluation(slug, value, "the server said so", null, null);
    }

    private static FeatureFlagEvaluator contextWith(ServerSideEvaluation... evaluations) {
        return new FeatureFlagEvaluator(new EvaluationResponse(List.of(evaluations), new byte[0]));
    }

    @Test
    void resolvesAFlagFromTheResponse() {
        var result = contextWith(serverResolved("feature-a", true)).evaluate("feature-a", null);

        assertThat(result.getValue()).isTrue();
        assertThat(result.getReason()).isEqualTo("the server said so");
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void resolvesADisabledFlagWithoutError() {
        // Off, not defaulted: the flag resolved, it just resolved to false.
        var result = contextWith(serverResolved("feature-a", false)).evaluate("feature-a", null);

        assertThat(result.getValue()).isFalse();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void matchesTheSlugWithoutRegardToCase() {
        var context = contextWith(serverResolved("Feature-A", true));

        assertThat(context.evaluate("feature-a", null).getValue()).isTrue();
        assertThat(context.evaluate("FEATURE-A", null).getValue()).isTrue();
    }

    @Test
    void picksTheRequestedFlagOutOfSeveral() {
        var context = contextWith(
                serverResolved("feature-a", true),
                serverResolved("feature-b", false));

        assertThat(context.evaluate("feature-a", null).getValue()).isTrue();
        assertThat(context.evaluate("feature-b", null).getValue()).isFalse();
    }

    @Test
    void anUnknownSlugThrowsFlagNotFound() {
        assertThatThrownBy(() -> contextWith(serverResolved("feature-a", true)).evaluate("no-such-flag", null))
                .isInstanceOf(FlagNotFoundError.class)
                .hasMessage("The slug provided did not match any of your Octopus Feature Flags. Please double check your slug and try again.")
                .extracting(thrown -> ((FlagNotFoundError) thrown).getErrorCode())
                .isEqualTo(ErrorCode.FLAG_NOT_FOUND);
    }

    @Test
    void aNullFlagKeyThrowsFlagNotFoundRatherThanFailing() {
        // The slug comes from the caller, so an unset config value arrives here as null. FLAG_NOT_FOUND
        // is the answer callers expect; anything else surfaces as ErrorCode.GENERAL with a raw exception
        // message.
        var context = contextWith(serverResolved("feature-a", true));

        assertThatThrownBy(() -> context.evaluate(null, null))
                .isInstanceOf(FlagNotFoundError.class)
                .extracting(thrown -> ((FlagNotFoundError) thrown).getErrorCode())
                .isEqualTo(ErrorCode.FLAG_NOT_FOUND);
        assertThat(context.findEvaluationBySlug(null)).isNull();
    }

    @Test
    void anEmptyResponseThrowsFlagNotFoundForEveryFlag() {
        assertThatThrownBy(() -> FeatureFlagEvaluator.empty().evaluate("feature-a", null))
                .isInstanceOf(FlagNotFoundError.class);
    }

    @Test
    void exposesTheContentHashOfTheResponseItHolds() {
        byte[] contentHash = {0x01, 0x02};

        assertThat(new FeatureFlagEvaluator(new EvaluationResponse(List.of(), contentHash)).getContentHash())
                .isEqualTo(contentHash);
        assertThat(FeatureFlagEvaluator.empty().getContentHash()).isEmpty();
    }

    @Test
    void aNullEntryInTheResponseDoesNotCostTheOtherFlags() throws Exception {
        // A malformed entry only fails its own flag, so a null alongside a well-formed flag must not take
        // the lookup down with it.
        List<ServerSideEvaluation> evaluations = OctopusObjectMapper.INSTANCE.readValue(
                Contexts.json("[ null, { 'slug': 'feature-a', 'value': true, 'reason': 'Enabled.' } ]"),
                new TypeReference<List<ServerSideEvaluation>>() {});
        var context = new FeatureFlagEvaluator(new EvaluationResponse(evaluations, new byte[0]));

        assertThat(context.evaluate("feature-a", null).getValue()).isTrue();
        assertThatThrownBy(() -> context.evaluate("no-such-flag", null))
                .as("the null entry is skipped rather than matched").isInstanceOf(FlagNotFoundError.class);
    }

    @Test
    void aResponseWithNoEvaluationsResolvesNothingRatherThanFailing() {
        // Defence in depth: the client turns a null body into a failed fetch, so this shape should not
        // reach the evaluator — but if it does, every flag is not-found rather than an NPE.
        var context = new FeatureFlagEvaluator(new EvaluationResponse(null, new byte[0]));

        assertThatThrownBy(() -> context.evaluate("feature-a", null)).isInstanceOf(FlagNotFoundError.class);
        assertThat(context.findEvaluationBySlug("feature-a")).isNull();
    }

    @Test
    void findsAnEvaluationBySlugWithoutEvaluatingIt() {
        var context = contextWith(serverResolved("feature-a", true));

        assertThat(context.findEvaluationBySlug("FEATURE-A")).isNotNull();
        assertThat(context.findEvaluationBySlug("no-such-flag")).isNull();
    }
}
