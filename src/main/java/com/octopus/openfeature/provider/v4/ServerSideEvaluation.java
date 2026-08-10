package com.octopus.openfeature.provider.v4;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

/**
 * A single feature flag as returned by the OctoToggle v4 evaluations endpoint. The endpoint returns
 * an array of these.
 *
 * <p>A flag is returned in one of two shapes:
 * <ul>
 * <li>Resolved by the server — {@code value} and {@code reason} are populated.</li>
 * <li>Deferred to the client — {@code evaluationKey} and {@code rules} are populated and the
 * provider library must evaluate the remaining client-side conditions.</li>
 * </ul>
 * Properties that do not apply to the returned shape are omitted from the JSON.
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
        this.rules = rules == null ? null : List.copyOf(rules);
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
}
