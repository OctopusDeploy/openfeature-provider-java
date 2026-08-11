package com.octopus.openfeature.provider.v4;

import dev.openfeature.sdk.EvaluationContext;

/**
 * What a flag's rules and conditions are evaluated against.
 */
final class ClientSideEvaluationContext {
    private final String evaluationKey;
    private final EvaluationContext openFeatureContext;

    ClientSideEvaluationContext(String evaluationKey, EvaluationContext openFeatureContext) {
        this.evaluationKey = evaluationKey;
        this.openFeatureContext = openFeatureContext;
    }

    /**
     * The key {@code percentage-by-context} buckets against.
     */
    String getEvaluationKey() {
        return evaluationKey;
    }

    /**
     * The caller's context, or {@code null} if they supplied none.
     */
    EvaluationContext getOpenFeatureContext() {
        return openFeatureContext;
    }
}
