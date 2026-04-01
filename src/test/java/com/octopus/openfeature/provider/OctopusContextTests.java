package com.octopus.openfeature.provider;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.Reason;
import dev.openfeature.sdk.exceptions.FlagNotFoundError;
import dev.openfeature.sdk.exceptions.ParseError;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class OctopusContextTests {

    private static final FeatureToggles sampleFeatureToggles = new FeatureToggles(
            Arrays.asList(
                    new FeatureToggleEvaluation("enabled-feature", true, UUID.randomUUID().toString(), Collections.emptyList(), 100),
                    new FeatureToggleEvaluation("disabled-feature", false, null, null, null),
                    new FeatureToggleEvaluation("feature-with-segments", true, UUID.randomUUID().toString(), Arrays.asList(new Segment("license-type", "free"), new Segment("country", "au")), 100)
            ),
            new byte[0]
    );


    @Test
    void shouldEvaluateToTrueIfFeatureToggleIsPresentAndEnabled() {
        var subject = new OctopusContext(sampleFeatureToggles);
        var result = subject.evaluate("enabled-feature", false, null);

        assertThat(result.getValue()).isTrue();
    }

    @Test
    void keyShouldBeCaseInsensitiveWhenEvaluating() {
        var subject = new OctopusContext(sampleFeatureToggles);
        var result = subject.evaluate("Enabled-Feature", false, null);

        assertThat(result.getValue()).isTrue();
    }

    @Test
    void shouldEvaluateToFalseIfFeatureToggleIsPresentAndDisabled() {
        var subject = new OctopusContext(sampleFeatureToggles);
        var result = subject.evaluate("disabled-feature", false, null);

        assertThat(result.getValue()).isFalse();
    }

    @Test
    void shouldThrowFlagNotFoundErrorIfFeatureToggleIsNotFound() {
        var defaultValue = false;
        var subject = new OctopusContext(sampleFeatureToggles);
        assertThrows(FlagNotFoundError.class, () -> subject.evaluate("key-not-present", defaultValue, null));
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

    @Test
    void whenTargetingKeyHashesToBucketWithinRolloutPercentage_evaluatesToTrue() {
        // "evaluation-key:targeting-key" is known to hash to bucket 13
        // rollout=13 → bucket(13) <= rollout(13) → within → true
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-x", true, "evaluation-key", Collections.emptyList(), 13)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        var ctx = new MutableContext();
        ctx.setTargetingKey("targeting-key");
        assertThat(subject.evaluate("feature-x", false, ctx).getValue()).isTrue();
    }

    @Test
    void whenTargetingKeyHashesToBucketOutsideRolloutPercentage_evaluatesToFalse() {
        // "evaluation-key:targeting-key" is known to hash to bucket 13
        // rollout=12 → bucket(13) > rollout(12) → outside → false
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-x", true, "evaluation-key", Collections.emptyList(), 12)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        var ctx = new MutableContext();
        ctx.setTargetingKey("targeting-key");
        assertThat(subject.evaluate("feature-x", false, ctx).getValue()).isFalse();
    }

    @Test
    void whenNoTargetingKeyAndRolloutLessThan100_evaluatesToFalse() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-x", true, "evaluation-key", Collections.emptyList(), 99)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("feature-x", false, new MutableContext()).getValue()).isFalse();
    }

    @Test
    void whenNoTargetingKeyAndRolloutIs100_evaluatesToTrue() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-x", true, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("feature-x", false, new MutableContext()).getValue()).isTrue();
    }

    @Test
    void whenTargetingKeyFallsWithinRolloutPercentage_andSegmentMatchesRequiredSegments_evaluatesToTrue() {
        // "evaluation-key:targeting-key" is known to hash to bucket 13
        // rollout=13 → within; segment license=trial matches → true
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-x", true, "evaluation-key", List.of(new Segment("license", "trial")), 13)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        var ctx = new MutableContext();
        ctx.setTargetingKey("targeting-key");
        ctx.add("license", "trial");
        assertThat(subject.evaluate("feature-x", false, ctx).getValue()).isTrue();
    }

    @Test
    void whenTargetingKeyFallsWithinRolloutPercentage_andSegmentValueDoesNotMatchRequiredSegment_evaluatesToFalse() {
        // "evaluation-key:targeting-key" is known to hash to bucket 13
        // rollout=99 → within; but required segment is license=enterprise, context has license=trial → false
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-x", true, "evaluation-key", List.of(new Segment("license", "enterprise")), 99)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        var ctx = new MutableContext();
        ctx.setTargetingKey("targeting-key");
        ctx.add("license", "trial");
        assertThat(subject.evaluate("feature-x", false, ctx).getValue()).isFalse();
    }

    @Test
    void whenTargetingKeyFallsOutsideRolloutPercentage_andSegmentValueDoesNotMatchRequiredSegment_evaluatesToFalse() {
        // "evaluation-key:targeting-key" is known to hash to bucket 13 > rollout=12 → outside → false
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-x", true, "evaluation-key", List.of(new Segment("license", "enterprise")), 12)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        var ctx = new MutableContext();
        ctx.setTargetingKey("targeting-key");
        ctx.add("license", "trial");
        assertThat(subject.evaluate("feature-x", false, ctx).getValue()).isFalse();
    }

    @Test
    void whenFeatureHasRequiredSegment_toleratesNullValuesInContext() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-x", true, "evaluation-key", List.of(new Segment("license", "trial")), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);

        // null value for the segment key → does not match
        var ctxNullLicense = new MutableContext();
        ctxNullLicense.add("license", (String) null);
        assertThat(subject.evaluate("feature-x", false, ctxNullLicense).getValue()).isFalse();

        // unrelated segment key → does not match
        var ctxOtherSegment = new MutableContext();
        ctxOtherSegment.add("other", "segment");
        assertThat(subject.evaluate("feature-x", false, ctxOtherSegment).getValue()).isFalse();

        // null context → does not match
        assertThat(subject.evaluate("feature-x", false, null).getValue()).isFalse();
    }

    @TestFactory
    Iterable<DynamicTest> shouldCorrectlyDynamicallyEvaluateSegmentsWhenSupplied() {
        return Arrays.asList(

                evaluationTest("no-segments", "enabled-feature",
                        buildEvaluationContext(List.of(
                                Map.entry("user-id", "123456")
                        )), true, Reason.DEFAULT.toString()),

                evaluationTest("no-context-values", "feature-with-segments",
                        buildEvaluationContext(List.of()
                        ), false, Reason.TARGETING_MATCH.toString()),

                evaluationTest("all-segments-match", "feature-with-segments",
                        buildEvaluationContext(Arrays.asList(
                                Map.entry("license-type", "free"), Map.entry("country", "au"))
                        ), true, Reason.TARGETING_MATCH.toString()),

                evaluationTest("extra-context-value", "feature-with-segments",
                        buildEvaluationContext(Arrays.asList(
                                Map.entry("license-type", "free"), Map.entry("country", "au"), Map.entry("user-id", "123456"))
                        ), true, Reason.TARGETING_MATCH.toString()),

                evaluationTest("context-values-do-not-match-segments", "feature-with-segments",
                        buildEvaluationContext(Arrays.asList(
                                Map.entry("license-type", "enterprise"), Map.entry("country", "au"))
                        ), false, Reason.TARGETING_MATCH.toString()),

                evaluationTest("context-value-not-supplied-for-all-segments", "feature-with-segments",
                        buildEvaluationContext(List.of(
                                Map.entry("license-type", "free"))
                        ), false, Reason.TARGETING_MATCH.toString())
        );
    }

    private DynamicTest evaluationTest(String testName, String featureToggleKey, EvaluationContext evaluationContext,
                                       boolean expectedResult, String expectedReason) {
        return DynamicTest.dynamicTest(testName, () -> {
            var subject = new OctopusContext(sampleFeatureToggles);
            var result = subject.evaluate(featureToggleKey, false, evaluationContext);
            assertThat(result.getValue()).isEqualTo(expectedResult);
            assertThat(result.getReason()).isEqualTo(expectedReason);
        });
    }

    private EvaluationContext buildEvaluationContext(List<Map.Entry<String, String>> entries) {
        var context = new MutableContext();
        entries.forEach(entry -> {
            context.add(entry.getKey(), entry.getValue());
        });
        return context;
    }

}
