package com.octopus.openfeature.provider;

/**
 * Reasons returned alongside a client-side evaluation. Both match the strings the Feature Flags service
 * produces server-side, so a flag reads the same whichever side resolved it.
 *
 * <p>These are deliberately not OpenFeature's {@link dev.openfeature.sdk.Reason} values, which is a
 * change in contract from v3: a caller that branched on {@code Reason.TARGETING_MATCH}, or that used the
 * reason as a metrics dimension, will see these sentences instead — and the set of them now grows with
 * the number of rule names.
 */
final class EvaluationReasons {

    private EvaluationReasons() {
    }

    static String matchedRule(String ruleName) {
        return "Matched rule '" + ruleName + "'.";
    }

    static String didNotMatchAnyRules() {
        return "Did not match any rules.";
    }
}
