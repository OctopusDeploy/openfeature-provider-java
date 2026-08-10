package com.octopus.openfeature.provider.v4;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.exceptions.ParseError;

import java.util.List;
import java.util.Optional;

/**
 * A single feature flag from the v4 evaluations endpoint, either resolved by the server
 * ({@code value} and {@code reason}) or deferred to the client ({@code evaluationKey} and
 * {@code rules}). Properties that do not apply to the returned shape are omitted from the JSON.
 */
final class ServerSideEvaluation {
    private final String slug;
    private final Boolean value;
    private final String reason;
    private final String evaluationKey;
    private final List<ClientSideRule> rules;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    ServerSideEvaluation(
            @JsonProperty(value = "slug", required = true) String slug,
            @JsonProperty("value") Boolean value,
            @JsonProperty("reason") String reason,
            @JsonProperty("evaluationKey") String evaluationKey,
            @JsonProperty("rules") List<ClientSideRule> rules
    ) {
        this.slug = slug;
        this.value = value;
        this.reason = reason;
        this.evaluationKey = evaluationKey;
        this.rules = WireLists.copyOrNull(rules);
    }

    public String getSlug() {
        return slug;
    }

    public Optional<Boolean> getValue() {
        return Optional.ofNullable(value);
    }

    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }

    public Optional<String> getEvaluationKey() {
        return Optional.ofNullable(evaluationKey);
    }

    public Optional<List<ClientSideRule>> getRules() {
        return Optional.ofNullable(rules);
    }

    /**
     * Resolves the flag, evaluating the client-side rules if the server left any: the flag is enabled
     * when any rule matches.
     *
     * <p>A response in neither shape throws {@link ParseError}, which the OpenFeature SDK turns into
     * the caller's default value.
     */
    ProviderEvaluation<Boolean> evaluate(EvaluationContext context) {
        if (value != null) {
            if (reason == null) {
                throw new ParseError("The flag has a value but has no reason.");
            }

            if (evaluationKey != null || rules != null) {
                throw new ParseError("The flag has both a server-resolved value and client-side rules.");
            }

            return resolved(value, reason);
        }

        if (rules == null) {
            throw new ParseError("The flag has neither a value nor rules.");
        }

        if (evaluationKey == null) {
            throw new ParseError("The flag defers to the client but has no evaluation key.");
        }

        if (rules.isEmpty()) {
            throw new ParseError("The flag defers to the client with no rules.");
        }

        var ruleContext = new ClientSideEvaluationContext(evaluationKey, context);

        for (ClientSideRule rule : rules) {
            if (rule == null) {
                throw new ParseError("The flag has a missing rule.");
            }

            if (rule.matches(ruleContext)) {
                return resolved(true, EvaluationReasons.matchedRule(rule.getName()));
            }
        }

        return resolved(false, EvaluationReasons.didNotMatchAnyRules());
    }

    private static ProviderEvaluation<Boolean> resolved(boolean value, String reason) {
        return ProviderEvaluation.<Boolean>builder()
                .value(value)
                .reason(reason)
                .build();
    }
}
