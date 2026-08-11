package com.octopus.openfeature.provider;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.exceptions.FlagNotFoundError;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds one evaluation response and resolves a flag from it, applying any client-side rules the
 * server deferred.
 */
class OctopusContext {

    private static final System.Logger logger = System.getLogger(OctopusContext.class.getName());

    private final EvaluationResponse evaluationResponse;
    private final Set<String> warnedSlugs = ConcurrentHashMap.newKeySet();

    OctopusContext(EvaluationResponse evaluationResponse) {
        this.evaluationResponse = evaluationResponse;
    }

    static OctopusContext empty() {
        return new OctopusContext(new EvaluationResponse(List.of(), new byte[0]));
    }

    byte[] getContentHash() {
        return evaluationResponse.getContentHash();
    }

    ServerSideEvaluation findEvaluationBySlug(String slug) {
        var evaluations = evaluationResponse.getEvaluations();
        if (slug == null || evaluations == null) {
            return null;
        }

        // A null entry carries no slug, so it can never be the flag being asked for. Skipping it keeps a
        // malformed entry from costing every other flag in the response.
        return evaluations.stream()
                .filter(Objects::nonNull)
                .filter(evaluation -> slug.equalsIgnoreCase(evaluation.getSlug()))
                .findFirst().orElse(null);
    }

    ProviderEvaluation<Boolean> evaluate(String slug, EvaluationContext evaluationContext) {
        var serverSideEvaluation = findEvaluationBySlug(slug);

        if (serverSideEvaluation == null) {
            // Warned once per slug: an unrecognised slug is usually a typo, which would otherwise log on
            // every evaluation of it.
            if (slug != null && warnedSlugs.add(slug.toLowerCase())) {
                logger.log(System.Logger.Level.WARNING, String.format(
                        "The slug %s did not match any of your Octopus Feature Flags. Please double check your slug and try again.",
                        slug));
            }

            throw new FlagNotFoundError(
                    "The slug provided did not match any of your Octopus Feature Flags. Please double check your slug and try again.");
        }

        return serverSideEvaluation.evaluate(evaluationContext);
    }
}
