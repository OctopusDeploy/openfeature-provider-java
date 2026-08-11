package com.octopus.openfeature.provider;

import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.MutableContext;
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
class OctopusContextTests {

    private static ServerSideEvaluation serverResolved(String slug, boolean value) {
        return new ServerSideEvaluation(slug, value, "the server said so", null, null);
    }

    private static OctopusContext contextWith(ServerSideEvaluation... evaluations) {
        return new OctopusContext(new EvaluationResponse(List.of(evaluations), new byte[0]));
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
    void anEmptyResponseThrowsFlagNotFoundForEveryFlag() {
        assertThatThrownBy(() -> OctopusContext.empty().evaluate("feature-a", null))
                .isInstanceOf(FlagNotFoundError.class);
    }

    @Test
    void appliesTheClientSideRulesTheServerDeferred() {
        var deferred = new ServerSideEvaluation("feature-a", null, null, "evaluation-key",
                List.of(new ClientSideRule("Pro plans",
                        List.of(new ContextAttributeIsOneOfCondition("plan", List.of("pro"))))));
        var context = contextWith(deferred);

        assertThat(context.evaluate("feature-a", new MutableContext().add("plan", "pro")).getValue())
                .as("the rule matches").isTrue();
        assertThat(context.evaluate("feature-a", new MutableContext().add("plan", "free")).getValue())
                .as("the rule does not match").isFalse();
    }

    @Test
    void exposesTheContentHashOfTheResponseItHolds() {
        byte[] contentHash = {0x01, 0x02};

        assertThat(new OctopusContext(new EvaluationResponse(List.of(), contentHash)).getContentHash())
                .isEqualTo(contentHash);
        assertThat(OctopusContext.empty().getContentHash()).isEmpty();
    }

    @Test
    void findsAnEvaluationBySlugWithoutEvaluatingIt() {
        var context = contextWith(serverResolved("feature-a", true));

        assertThat(context.findEvaluationBySlug("FEATURE-A")).isNotNull();
        assertThat(context.findEvaluationBySlug("no-such-flag")).isNull();
    }
}
