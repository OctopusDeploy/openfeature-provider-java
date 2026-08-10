package com.octopus.openfeature.provider.v4;

/**
 * Reasons returned alongside a client-side evaluation. Both match the strings the Feature Flags service
 * produces server-side, so a flag reads the same whichever side resolved it.
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
