package com.octopus.openfeature.provider.v4;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A named rule the provider library still has to evaluate on the client side. The rule matches when
 * every one of its conditions matches.
 */
final class ClientSideRule {
    private final String name;
    private final List<ClientSideCondition> conditions;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    ClientSideRule(
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "conditions", required = true) List<ClientSideCondition> conditions
    ) {
        this.name = name;
        this.conditions = List.copyOf(conditions);
    }

    public String getName() {
        return name;
    }

    public List<ClientSideCondition> getConditions() {
        return conditions;
    }
}
