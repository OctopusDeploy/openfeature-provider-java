package com.octopus.openfeature.provider;

import java.util.List;

// TODO(BMBB-780): a v3 type, unused since the switch to v4.
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
