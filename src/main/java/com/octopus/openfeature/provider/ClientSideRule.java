package com.octopus.openfeature.provider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.openfeature.sdk.exceptions.ParseError;

import java.util.List;

/**
 * A named rule the provider library evaluates on the client side. The rule matches when every one of
 * its conditions matches.
 */
final class ClientSideRule {
    private final String name;
    private final List<ClientSideCondition> conditions;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    ClientSideRule(
            @JsonProperty("name") String name,
            @JsonProperty("conditions") List<ClientSideCondition> conditions
    ) {
        this.name = name;
        this.conditions = ListUtils.copyOrNull(conditions);
    }

    public String getName() {
        return name;
    }

    public List<ClientSideCondition> getConditions() {
        return conditions;
    }

    boolean matches(ClientSideEvaluationContext context) {
        // The server only defers a named rule carrying at least one condition, so anything else is a
        // response it could not have sent.
        if (name == null) {
            throw new ParseError("A rule has no name.");
        }

        if (conditions == null || conditions.isEmpty()) {
            throw new ParseError("Rule '" + name + "' has no conditions.");
        }

        for (ClientSideCondition condition : conditions) {
            if (condition == null) {
                throw new ParseError("Rule '" + name + "' has a missing condition.");
            }

            if (!condition.matches(context)) {
                return false;
            }
        }

        return true;
    }
}
