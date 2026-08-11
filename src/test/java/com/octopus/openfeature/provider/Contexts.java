package com.octopus.openfeature.provider;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.MutableContext;

/**
 * Builds the contexts the v4 evaluation tests run against.
 */
final class Contexts {

    // getNormalizedNumber("evaluation-key", "targeting-key") == 13, so this targeting key is inside a
    // >=13% rollout and outside a <13% one. The rollout tests either side of the bucket pin that value,
    // which is also what the .NET provider's equivalent tests use.
    static final String SLUG = "my-feature";
    static final String EVALUATION_KEY = "evaluation-key";
    static final String TARGETING_KEY = "targeting-key";
    static final int TARGETING_KEY_BUCKET = 13;

    private Contexts() {
    }

    /**
     * A JSON payload written with single quotes, so the malformed-response cases stay readable on one
     * line without escaping. No payload here contains an apostrophe.
     */
    static String json(String singleQuoted) {
        return singleQuoted.replace('\'', '"');
    }

    /**
     * An OpenFeature context with the given targeting key and string attributes, supplied as
     * alternating key and value arguments.
     */
    static EvaluationContext openFeature(String targetingKey, String... attributes) {
        if (attributes.length % 2 != 0) {
            throw new IllegalArgumentException("Attributes must be alternating keys and values.");
        }

        var context = new MutableContext();
        for (int i = 0; i < attributes.length; i += 2) {
            context.add(attributes[i], attributes[i + 1]);
        }

        if (targetingKey != null) {
            context.setTargetingKey(targetingKey);
        }

        return context;
    }

    /**
     * What a rule or condition is evaluated against.
     */
    static ClientSideEvaluationContext forRules(String targetingKey, String... attributes) {
        return new ClientSideEvaluationContext(EVALUATION_KEY, openFeature(targetingKey, attributes));
    }

    /**
     * A rule context whose caller supplied no context at all.
     */
    static ClientSideEvaluationContext withoutOpenFeatureContext() {
        return new ClientSideEvaluationContext(EVALUATION_KEY, null);
    }
}
