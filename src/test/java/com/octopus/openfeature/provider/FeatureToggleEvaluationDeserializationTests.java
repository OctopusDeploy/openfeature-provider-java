package com.octopus.openfeature.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureToggleEvaluationDeserializationTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private InputStream resource(String name) {
        return getClass().getResourceAsStream(name);
    }

    @Test
    void shouldDeserializeEnabledToggle() throws Exception {
        FeatureToggleEvaluation result = objectMapper.readValue(
                resource("toggle-enabled-no-segments.json"), FeatureToggleEvaluation.class);

        assertThat(result.getName()).isEqualTo("My Feature");
        assertThat(result.getSlug()).isEqualTo("my-feature");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getSegments()).isEmpty();
    }

    @Test
    void shouldDeserializeDisabledToggle() throws Exception {
        FeatureToggleEvaluation result = objectMapper.readValue(
                resource("toggle-disabled.json"), FeatureToggleEvaluation.class);

        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void shouldDeserializeToggleWithMissingSegmentsField() throws Exception {
        FeatureToggleEvaluation result = objectMapper.readValue(
                resource("toggle-missing-segments.json"), FeatureToggleEvaluation.class);

        assertThat(result.getSegments()).isNotNull().isEmpty();
    }

    @Test
    void shouldDeserializeToggleWithSegments() throws Exception {
        FeatureToggleEvaluation result = objectMapper.readValue(
                resource("toggle-with-segments.json"), FeatureToggleEvaluation.class);

        assertThat(result.getSegments()).hasSize(2);
        assertThat(result.getSegments()).contains(
                Map.entry("license-type", "free"),
                Map.entry("country", "au")
        );
    }

    @Test
    void shouldDeserializeListOfToggles() throws Exception {
        List<FeatureToggleEvaluation> result = objectMapper.readValue(
                resource("toggle-list.json"), new TypeReference<List<FeatureToggleEvaluation>>() {});

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSlug()).isEqualTo("feature-a");
        assertThat(result.get(0).isEnabled()).isTrue();
        assertThat(result.get(1).getSlug()).isEqualTo("feature-b");
        assertThat(result.get(1).isEnabled()).isFalse();
    }

    @Test
    void shouldIgnoreExtraneousProperties() throws Exception {
        FeatureToggleEvaluation result = objectMapper.readValue(
                resource("toggle-with-extraneous-properties.json"), FeatureToggleEvaluation.class);

        assertThat(result.getName()).isEqualTo("My Feature");
        assertThat(result.getSlug()).isEqualTo("my-feature");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getSegments()).isEmpty();
    }
}
