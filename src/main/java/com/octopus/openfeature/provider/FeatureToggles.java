package com.octopus.openfeature.provider;

import java.util.List;

class FeatureToggles {
    private final List<FeatureToggleEvaluation> evaluations;
    private final byte[] contentHash;

    FeatureToggles(List<FeatureToggleEvaluation> evaluations, byte[] contentHash) {
        this.evaluations = evaluations;
        this.contentHash = contentHash;
    }

    public List<FeatureToggleEvaluation> getEvaluations() {
        return evaluations;
    }

    public byte[] getContentHash() {
        return contentHash;
    }
}
