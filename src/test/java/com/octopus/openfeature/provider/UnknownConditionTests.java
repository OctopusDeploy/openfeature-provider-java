package com.octopus.openfeature.provider;

import dev.openfeature.sdk.exceptions.ParseError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnknownConditionTests {

    @Test
    void anUnrecognisedTypeNeverMatches() {
        assertThat(new UnknownCondition("some-future-condition")
                .matches(Contexts.forRules(Contexts.TARGETING_KEY))).isFalse();
    }

    @Test
    void noTypeAtAllThrowsAParseError() {
        // No server version emits a condition without a type, unlike one with a type we do not know.
        assertThatThrownBy(() -> new UnknownCondition(null)
                .matches(Contexts.forRules(Contexts.TARGETING_KEY)))
                .isInstanceOf(ParseError.class)
                .hasMessage("A condition is missing a type.");
    }
}
