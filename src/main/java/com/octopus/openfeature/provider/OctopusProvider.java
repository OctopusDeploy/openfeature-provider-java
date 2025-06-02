package com.octopus.openfeature.provider;

import dev.openfeature.sdk.*;

public class OctopusProvider extends EventProvider {
    private static final String PROVIDER_NAME = "octopus";
    private final OctopusConfiguration config;
    private final OctopusContextProvider contextProvider;
    
    public OctopusProvider(OctopusConfiguration config) {
       this.config = config; 
       this.contextProvider = new OctopusContextProvider(config, new OctopusClient(config));
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
    public ProviderEvaluation<String> getStringEvaluation(String s, String s1, EvaluationContext evaluationContext) {
        throw new UnsupportedOperationException("Only boolean values are currently supported");
    }

    @Override
    public ProviderEvaluation<Integer> getIntegerEvaluation(String s, Integer integer, EvaluationContext evaluationContext) {
        throw new UnsupportedOperationException("Only boolean values are currently supported");
    }

    @Override
    public ProviderEvaluation<Double> getDoubleEvaluation(String s, Double aDouble, EvaluationContext evaluationContext) {
        throw new UnsupportedOperationException("Only boolean values are currently supported");
    }

    @Override
    public ProviderEvaluation<Value> getObjectEvaluation(String s, Value value, EvaluationContext evaluationContext) {
        throw new UnsupportedOperationException("Only boolean values are currently supported");
    }
}
