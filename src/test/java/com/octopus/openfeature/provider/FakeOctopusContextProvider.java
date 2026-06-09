package com.octopus.openfeature.provider;

// For testing: pre-loads a known context, skipping HTTP fetch and background refresh.
class FakeOctopusContextProvider extends OctopusContextProvider {
    private final OctopusContext context;

    FakeOctopusContextProvider(OctopusContext context) {
        super(null, null);
        this.context = context;
    }

    @Override
    OctopusContext getOctopusContext() { return context; }

    @Override
    void initialize() { }

    @Override
    void shutdown() { }
}
