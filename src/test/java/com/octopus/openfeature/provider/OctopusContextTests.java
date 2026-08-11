package com.octopus.openfeature.provider;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.exceptions.FlagNotFoundError;
import dev.openfeature.sdk.exceptions.ParseError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class OctopusContextTests {

    @Test
    void evaluatesToTrue_IfFeatureIsContainedWithinTheSet_AndFeatureIsEnabled() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, null).getValue()).isTrue();
    }

    @Test
    void whenEvaluatedWithCasingDifferences_EvaluationIsInsensitiveToCase() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("Test-Feature", false, null).getValue()).isTrue();
    }

    @Test
    void evaluatesToFalse_IfFeatureIsContainedWithinTheSet_AndFeatureIsNotEnabled() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", false, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, null).getValue()).isFalse();
    }

    @Test
    void givenAFlagKeyThatIsNotASlug_ThrowsFlagNotFound() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("this-is-clearly-not-a-slug", true, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThrows(FlagNotFoundError.class, () -> subject.evaluate("This is clearly not a slug!", true, null));
    }

    @Test
    void throwsFlagNotFound_IfFeatureIsNotContainedWithinSet() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("testfeature", false, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThrows(FlagNotFoundError.class, () -> subject.evaluate("anotherfeature", true, null));
    }

    @Test
    void whenAFeatureIsToggledOnForASpecificSegment_EvaluatesToTrueWhenSegmentIsSpecified() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("testfeature", true, "evaluation-key", List.of(new Segment("license", "trial")), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);

        assertThat(subject.evaluate("testfeature", false, buildContext(List.of(Map.entry("license", "trial")))).getValue()).isTrue();
        assertThat(subject.evaluate("testfeature", false, buildContext(List.of(Map.entry("other", "segment")))).getValue()).isFalse();
        assertThat(subject.evaluate("testfeature", false, null).getValue()).isFalse();
    }

    @Test
    void whenFeatureIsNotToggledOnForSpecificSegments_EvaluatesToTrueRegardlessOfSegmentSpecified() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("testfeature", true, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);

        assertThat(subject.evaluate("testfeature", false, buildContext(List.of(Map.entry("license", "trial")))).getValue()).isTrue();
        assertThat(subject.evaluate("testfeature", false, null).getValue()).isTrue();
    }

    @Test
    void whenAFeatureIsToggledOnForMultipleSegments_EvaluatesCorrectly() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation(
                        "testfeature", true, "evaluation-key",
                        Arrays.asList(new Segment("license", "trial"), new Segment("region", "au"), new Segment("region", "us")),
                        100
                )),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);

        // A matching context value is present for each toggled segment
        assertThat(subject.evaluate("testfeature", false, buildContext(Arrays.asList(Map.entry("license", "trial"), Map.entry("region", "us")))).getValue())
                .isTrue();

        // A context value is present for each toggled segment, but it is not toggled on for one of the supplied values
        assertThat(subject.evaluate("testfeature", false, buildContext(Arrays.asList(Map.entry("license", "trial"), Map.entry("region", "eu")))).getValue())
                .isFalse();

        // A matching context value is present for each toggled segment, and an additional segment is present in the provided context values
        assertThat(subject.evaluate("testfeature", false, buildContext(Arrays.asList(Map.entry("license", "trial"), Map.entry("region", "us"), Map.entry("language", "english")))).getValue())
                .isTrue();

        // A context value is present for only one of the two toggled segments
        assertThat(subject.evaluate("testfeature", false, buildContext(List.of(Map.entry("license", "trial")))).getValue())
                .isFalse();

        // No context values are present for the two toggled segments
        assertThat(subject.evaluate("testfeature", true, buildContext(List.of(Map.entry("other", "segment")))).getValue())
                .isFalse();

        // None specified
        assertThat(subject.evaluate("testfeature", true, null).getValue())
                .isFalse();
    }

    @Test
    void whenAFeatureIsToggledOnForASpecificSegment_ToleratesNullValuesInContext() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("testfeature", true, "evaluation-key", List.of(new Segment("license", "trial")), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);

        // null value for the segment key does not match
        var ctxNullLicense = new MutableContext();
        ctxNullLicense.add("license", (String) null);
        assertThat(subject.evaluate("testfeature", false, ctxNullLicense).getValue()).isFalse();

        assertThat(subject.evaluate("testfeature", false, buildContext(List.of(Map.entry("other", "segment")))).getValue()).isFalse();
        assertThat(subject.evaluate("testfeature", false, null).getValue()).isFalse();
    }

    @Test
    void whenTargetingKeyFallsWithinRolloutPercentage_AndFeatureIsNotToggledForSegments_ResolvesToTrue() {
        // "evaluation-key:targeting-key" hashes to bucket 13, which is within the rollout of 13, so the feature is enabled
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 13)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(Collections.emptyList(), "targeting-key")).getValue()).isTrue();
    }

    @Test
    void whenTargetingKeyFallsOutsideRolloutPercentage_AndFeatureIsNotToggledForSegments_ResolvesToFalse() {
        // "evaluation-key:targeting-key" hashes to bucket 13, which exceeds the rollout of 12, so the feature is disabled
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 12)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(Collections.emptyList(), "targeting-key")).getValue()).isFalse();
    }

    @Test
    void whenTargetingKeyFallsWithinRolloutPercentage_AndSegmentMatchesRequiredSegments_EvaluatesToTrue() {
        // "evaluation-key:targeting-key" hashes to bucket 13, which is within the rollout of 13, and the segment license=trial matches
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", List.of(new Segment("license", "trial")), 13)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(List.of(Map.entry("license", "trial")), "targeting-key")).getValue()).isTrue();
    }

    @Test
    void whenTargetingKeyFallsWithinRolloutPercentage_AndSegmentValueDoesNotMatchRequiredSegment_EvaluatesToFalse() {
        // "evaluation-key:targeting-key" hashes to bucket 13, which is within the rollout of 99, but the required
        // segment license=enterprise does not match the provided license=trial, so the feature is disabled
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", List.of(new Segment("license", "enterprise")), 99)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(List.of(Map.entry("license", "trial")), "targeting-key")).getValue()).isFalse();
    }

    @Test
    void whenTargetingKeyFallsOutsideRolloutPercentage_AndSegmentValueDoesNotMatchRequiredSegment_EvaluatesToFalse() {
        // "evaluation-key:targeting-key" hashes to bucket 13, which exceeds the rollout of 12, and the segment also does not match
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", List.of(new Segment("license", "enterprise")), 12)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(List.of(Map.entry("license", "trial")), "targeting-key")).getValue()).isFalse();
    }

    @Test
    void whenNoTargetingKey_RolloutIsLessThanOneHundredPercent_ResolvesToFalse() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 99)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(Collections.emptyList(), null)).getValue()).isFalse();
    }

    @Test
    void whenNoTargetingKey_RolloutIsEqualToOneHundredPercent_ResolvesToTrue() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(Collections.emptyList(), null)).getValue()).isTrue();
    }

    @Test
    void shouldThrowParseErrorWhenEnabledToggleIsMissingEvaluationKey() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-a", true, null, Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        var ex = assertThrows(ParseError.class, () -> subject.evaluate("feature-a", false, null));
        assertThat(ex.getMessage()).contains("feature-a");
    }

    @Test
    void shouldThrowParseErrorWhenEnabledToggleIsMissingSegments() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-b", true, "evaluation-key", null, 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        var ex = assertThrows(ParseError.class, () -> subject.evaluate("feature-b", false, null));
        assertThat(ex.getMessage()).contains("feature-b");
    }

    @Test
    void shouldThrowParseErrorWhenEnabledToggleIsMissingClientRolloutPercentage() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-c", true, "evaluation-key", Collections.emptyList(), null)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        var ex = assertThrows(ParseError.class, () -> subject.evaluate("feature-c", false, null));
        assertThat(ex.getMessage()).contains("feature-c");
    }

    @Test
    void shouldThrowParseErrorWhenEnabledToggleIsMissingAllClientEvaluationFields() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-d", true, null, null, null)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        var ex = assertThrows(ParseError.class, () -> subject.evaluate("feature-d", true, null));
        assertThat(ex.getMessage()).contains("feature-d");
    }

    private EvaluationContext buildContext(List<Map.Entry<String, String>> entries) {
        return buildContext(entries, null);
    }

    private EvaluationContext buildContext(List<Map.Entry<String, String>> entries, String targetingKey) {
        var context = new MutableContext();
        entries.forEach(entry -> context.add(entry.getKey(), entry.getValue()));
        if (targetingKey != null) {
            context.setTargetingKey(targetingKey);
        }
        return context;
    }

    // The vectors live in RolloutVectors, shared with the v4 rollout so both implementations are held
    // to the same expected buckets.
    @ParameterizedTest(name = "[{index}] ({0}, {1}) -> {2}")
    @MethodSource("com.octopus.openfeature.provider.RolloutVectors#cases")
    void getNormalizedNumberMatchesExpectedValue(String evaluationKey, String targetingKey, int expected) {
        assertThat(OctopusContext.getNormalizedNumber(evaluationKey, targetingKey)).isEqualTo(expected);
    }

}
