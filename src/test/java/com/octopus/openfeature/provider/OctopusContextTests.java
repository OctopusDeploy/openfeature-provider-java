package com.octopus.openfeature.provider;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.Reason;
import dev.openfeature.sdk.exceptions.FlagNotFoundError;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class OctopusContextTests {
    
    private static final FeatureToggles sampleFeatureToggles = new FeatureToggles(
            Arrays.asList(
                    new FeatureToggleEvaluation("Enabled Feature", "enabled-feature", true, null),
                    new FeatureToggleEvaluation("Disabled Feature", "disabled-feature", false, null),
                    new FeatureToggleEvaluation("Feature With Segments", "feature-with-segments", true, Arrays.asList(Map.entry("license-type", "free"), Map.entry("country", "au")) )
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
