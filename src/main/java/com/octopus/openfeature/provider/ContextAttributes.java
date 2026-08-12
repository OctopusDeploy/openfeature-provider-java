package com.octopus.openfeature.provider;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.exceptions.ParseError;

import java.util.List;
import java.util.Objects;

/**
 * The attribute lookup shared by both attribute conditions.
 */
final class ContextAttributes {

    private ContextAttributes() {
    }

    /**
     * Whether the context holds an attribute named {@code key} with one of {@code values}.
     *
     * <p>Keys and values compare case-insensitively and a non-string value counts as absent. Every
     * entry whose key matches is checked, not just the first — a context can hold several case
     * variants of one key.
     */
    static boolean isOneOf(ClientSideEvaluationContext context, String key, List<String> values) {
        if (key == null) {
            throw new ParseError("A condition is missing a key.");
        }

        if (values == null || values.isEmpty()) {
            throw new ParseError("A condition is missing values.");
        }

        if (values.stream().anyMatch(Objects::isNull)) {
            throw new ParseError("A condition is missing a value.");
        }

        EvaluationContext openFeatureContext = context.getOpenFeatureContext();
        if (openFeatureContext == null) {
            return false;
        }

        return openFeatureContext.asMap().entrySet().stream().anyMatch(entry -> {
            if (!entry.getKey().equalsIgnoreCase(key)) {
                return false;
            }

            String attribute = entry.getValue().asString();
            return attribute != null && values.stream().anyMatch(value -> value.equalsIgnoreCase(attribute));
        });
    }
}
