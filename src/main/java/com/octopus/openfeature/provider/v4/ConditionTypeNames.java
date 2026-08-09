package com.octopus.openfeature.provider.v4;

/**
 * Discriminator values for the polymorphic v4 client-side conditions. These mirror the values in the
 * evaluation response, so they must not drift from the server.
 */
final class ConditionTypeNames {
    static final String CONTEXT_ATTRIBUTE_IS_NOT_ONE_OF = "context-attribute-is-not-one-of";
    static final String CONTEXT_ATTRIBUTE_IS_ONE_OF = "context-attribute-is-one-of";
    static final String PERCENTAGE_BY_CONTEXT = "percentage-by-context";

    private ConditionTypeNames() {
    }
}
