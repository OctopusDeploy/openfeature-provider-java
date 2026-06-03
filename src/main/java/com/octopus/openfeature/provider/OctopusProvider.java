package com.octopus.openfeature.provider;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.EventProvider;
import dev.openfeature.sdk.Metadata;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.exceptions.FlagNotFoundError;
import dev.openfeature.sdk.exceptions.TypeMismatchError;

public class OctopusProvider extends EventProvider {
    private static final String PROVIDER_NAME = "octopus-java-provider";
    private final OctopusConfiguration config;
    private final OctopusContextProvider contextProvider;
    
    public OctopusProvider(OctopusConfiguration config) {
       this.config = config;
       this.contextProvider = new OctopusContextProvider(config, new OctopusClient(config));
    }

    // For unit testing: accepts a pre-built context provider instead of constructing one from config.
    OctopusProvider(OctopusContextProvider contextProvider) {
        this.config = null;
        this.contextProvider = contextProvider;
    }

    @Override
    public Metadata getMetadata() { return () -> PROVIDER_NAME; }

    @Override
    public void initialize(EvaluationContext evaluationContext) throws Exception {
        super.initialize(evaluationContext);
        contextProvider.initialize();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        contextProvider.shutdown();
    }

    @Override
    public ProviderEvaluation<Boolean> getBooleanEvaluation(String flagKey, Boolean defaultValue, EvaluationContext evaluationContext) {
        return contextProvider.getOctopusContext().evaluate(flagKey, defaultValue, evaluationContext);
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
        var toggle = contextProvider.getOctopusContext().findFeatureToggleBySlug(flagKey);
        if (toggle == null) {
            return new FlagNotFoundError(flagKey);
        }
        return new TypeMismatchError("Octopus Feature Toggles only supports boolean toggles.");
    }
}
