package com.octopus.openfeature.provider;

import java.util.List;

/**
 * One response from the v4 evaluations endpoint: the server-side evaluation of every flag, and the
 * content hash identifying that set.
 */
class EvaluationResponse {
    private final List<ServerSideEvaluation> evaluations;
    private final byte[] contentHash;

    EvaluationResponse(List<ServerSideEvaluation> evaluations, byte[] contentHash) {
        this.evaluations = evaluations;
        this.contentHash = contentHash;
    }

    public List<ServerSideEvaluation> getEvaluations() {
        return evaluations;
    }

    public byte[] getContentHash() {
        return contentHash;
    }
}
