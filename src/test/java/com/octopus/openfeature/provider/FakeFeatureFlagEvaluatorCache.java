package com.octopus.openfeature.provider;

// For testing: pre-loads a known evaluator, skipping HTTP fetch and background refresh.
class FakeFeatureFlagEvaluatorCache extends FeatureFlagEvaluatorCache {
    private final FeatureFlagEvaluator evaluator;

    FakeFeatureFlagEvaluatorCache(FeatureFlagEvaluator evaluator) {
        super(null, null);
        this.evaluator = evaluator;
    }

    @Override
    FeatureFlagEvaluator getEvaluator() { return evaluator; }

    @Override
    void initialize() { }

    @Override
    void shutdown() { }
}
