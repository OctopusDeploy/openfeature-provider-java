package com.octopus.openfeature.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureToggleEvaluationDeserializationTests {

    private final ObjectMapper objectMapper = OctopusObjectMapper.create();

    private InputStream resource(String name) {
        return getClass().getResourceAsStream(name);
    }

    private void assertSegmentsContain(List<Segment> segments, Segment... expected) {
        assertThat(segments).usingRecursiveFieldByFieldElementComparator().contains(expected);
    }

    @Test
    void shouldDeserializeEnabledToggle() throws Exception {
        FeatureToggleEvaluation result = objectMapper.readValue(resource("toggle-enabled-no-segments.json"), FeatureToggleEvaluation.class);

        assertThat(result.getName()).isEqualTo("My Feature");
        assertThat(result.getSlug()).isEqualTo("my-feature");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getSegments()).isEmpty();
    }

    @Test
    void shouldDeserializeDisabledToggle() throws Exception {
        FeatureToggleEvaluation result = objectMapper.readValue(resource("toggle-disabled.json"), FeatureToggleEvaluation.class);

        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void shouldDeserializeToggleWithMissingSegmentsField() throws Exception {
        FeatureToggleEvaluation result = objectMapper.readValue(resource("toggle-missing-segments.json"), FeatureToggleEvaluation.class);

        assertThat(result.getSegments()).isNotNull().isEmpty();
    }

    @Test
    void shouldDeserializeToggleWithSegments() throws Exception {
        FeatureToggleEvaluation result = objectMapper.readValue(
                resource("toggle-with-segments.json"), FeatureToggleEvaluation.class);

        assertThat(result.getSegments()).hasSize(2);
        assertSegmentsContain(result.getSegments(),
                new Segment("license-type", "free"),
                new Segment("country", "au")
        );
    }

    @Test
    void shouldDeserializeListOfToggles() throws Exception {
        List<FeatureToggleEvaluation> result = objectMapper.readValue(
                resource("toggle-list.json"),
                new TypeReference<>() {
                }
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSlug()).isEqualTo("feature-a");
        assertThat(result.get(0).isEnabled()).isTrue();
        assertThat(result.get(1).getSlug()).isEqualTo("feature-b");
        assertThat(result.get(1).isEnabled()).isFalse();
    }

    @Test
    void shouldDeserializeListOfTogglesWithVariousFieldCasings() throws Exception {
        List<FeatureToggleEvaluation> result = objectMapper.readValue(
                resource("toggles-with-different-field-capitalisation.json"),
                new TypeReference<>() {
                }
        );

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getSlug()).isEqualTo("feature-a");
        assertThat(result.get(0).isEnabled()).isTrue();
        assertSegmentsContain(result.get(0).getSegments(), new Segment("license-type", "free"));
        assertThat(result.get(1).getSlug()).isEqualTo("feature-b");
        assertThat(result.get(1).isEnabled()).isTrue();
        assertSegmentsContain(result.get(1).getSegments(), new Segment("plan", "enterprise"));
        assertThat(result.get(2).getSlug()).isEqualTo("feature-c");
        assertThat(result.get(2).isEnabled()).isTrue();
        assertSegmentsContain(result.get(2).getSegments(), new Segment("country", "au"));
    }

    @Test
    void shouldIgnoreExtraneousProperties() throws Exception {
        FeatureToggleEvaluation result = objectMapper.readValue(
                resource("toggle-with-extraneous-properties.json"), FeatureToggleEvaluation.class);

        assertThat(result.getName()).isEqualTo("My Feature");
        assertThat(result.getSlug()).isEqualTo("my-feature");
        assertThat(result.isEnabled()).isTrue();
        assertSegmentsContain(result.getSegments(), new Segment("license-type", "free"));
    }
}
