package com.octopus.openfeature.provider;

import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.FlagNotFoundError;
import dev.openfeature.sdk.exceptions.TypeMismatchError;

public class OctopusProvider extends EventProvider {
    private static final String PROVIDER_NAME = "octopus-java-provider";
    private final OctopusConfiguration config;
    private final FeatureFlagEvaluatorCache evaluatorCache;
    
    public OctopusProvider(OctopusConfiguration config) {
       this.config = config; 
       this.evaluatorCache = new FeatureFlagEvaluatorCache(config, new FeatureFlagApiClient(config));
    }

    // For testing: accepts a pre-built evaluator cache instead of constructing one from config.
    OctopusProvider(FeatureFlagEvaluatorCache evaluatorCache) {
        this.config = null;
        this.evaluatorCache = evaluatorCache;
    }

    @Override
    public Metadata getMetadata() { return () -> PROVIDER_NAME; }

    @Override
    public void initialize(EvaluationContext evaluationContext) throws Exception {
        super.initialize(evaluationContext);
        evaluatorCache.initialize();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        evaluatorCache.shutdown();
    }

    @Override
    public ProviderEvaluation<Boolean> getBooleanEvaluation(String flagKey, Boolean defaultValue, EvaluationContext evaluationContext) {
        return evaluatorCache.getEvaluator().evaluate(flagKey, evaluationContext);
    }

    @Override
    public ProviderEvaluation<String> getStringEvaluation(String flagKey, String defaultValue, EvaluationContext evaluationContext) {
        throw rejectNonBooleanEvaluation(flagKey);
    }

    @Override
    public ProviderEvaluation<Integer> getIntegerEvaluation(String flagKey, Integer defaultValue, EvaluationContext evaluationContext) {
        throw rejectNonBooleanEvaluation(flagKey);
    }

    @Override
    public ProviderEvaluation<Double> getDoubleEvaluation(String flagKey, Double defaultValue, EvaluationContext evaluationContext) {
        throw rejectNonBooleanEvaluation(flagKey);
    }

    @Override
    public ProviderEvaluation<Value> getObjectEvaluation(String flagKey, Value defaultValue, EvaluationContext evaluationContext) {
        throw rejectNonBooleanEvaluation(flagKey);
    }

    private RuntimeException rejectNonBooleanEvaluation(String flagKey) {
        var evaluation = evaluatorCache.getEvaluator().findEvaluationBySlug(flagKey);
        if (evaluation == null) {
            return new FlagNotFoundError(
                    "The slug provided did not match any of your Octopus Feature Flags. Please double check your slug and try again.");
        }
        return new TypeMismatchError("Octopus only supports boolean flags.");
    }
}
