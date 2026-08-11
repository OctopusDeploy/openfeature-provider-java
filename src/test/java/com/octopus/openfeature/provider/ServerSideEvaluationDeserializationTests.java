package com.octopus.openfeature.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Deserialization of a v4 evaluation response: both flag shapes and the array the endpoint returns.
 * Uses the provider's own {@code OctopusObjectMapper}, as the client does in production; individual
 * conditions are covered by {@link ClientSideConditionDeserializationTests}.
 */
class ServerSideEvaluationDeserializationTests {

    private final ObjectMapper objectMapper = OctopusObjectMapper.INSTANCE;

    private InputStream resource(String name) {
        return getClass().getResourceAsStream(name);
    }

    @Test
    void shouldDeserializeServerResolvedEvaluation() throws Exception {
        var evaluation = objectMapper.readValue(
                resource("v4-evaluation-server-resolved.json"), ServerSideEvaluation.class);

        assertThat(evaluation.getSlug()).isEqualTo("my-feature");
        assertThat(evaluation.getValue()).hasValue(true);
        assertThat(evaluation.getReason()).hasValue("The flag is enabled for this environment.");
        assertThat(evaluation.getEvaluationKey()).isEmpty();
        assertThat(evaluation.getRules()).isEmpty();
    }

    @Test
    void shouldDeserializeEvaluationDeferredToTheClientWithPolymorphicConditions() throws Exception {
        var evaluation = objectMapper.readValue(
                resource("v4-evaluation-deferred-to-client.json"), ServerSideEvaluation.class);

        assertThat(evaluation.getSlug()).isEqualTo("my-feature");
        assertThat(evaluation.getEvaluationKey()).hasValue("0f8fad5b-d9cb-469f-a165-70867728950e");
        assertThat(evaluation.getValue()).isEmpty();
        assertThat(evaluation.getReason()).isEmpty();

        var rules = evaluation.getRules().orElseThrow();
        assertThat(rules).hasSize(1);

        var rule = rules.get(0);
        assertThat(rule.getName()).isEqualTo("Rule 1");
        assertThat(rule.getConditions()).hasSize(2);

        assertThat(rule.getConditions().get(0))
                .isInstanceOfSatisfying(PercentageByContextCondition.class,
                        percentage -> assertThat(percentage.getPercentage()).hasValue(50));
        assertThat(rule.getConditions().get(1))
                .isInstanceOfSatisfying(ContextAttributeIsOneOfCondition.class, isOneOf -> {
                    assertThat(isOneOf.getKey()).isEqualTo("user-id");
                    assertThat(isOneOf.getValues()).containsExactly("1234", "5678");
                });
    }

    @Test
    void shouldPreserveUnknownConditionAlongsideKnownConditionsWithoutFailingTheResponse() throws Exception {
        var evaluation = objectMapper.readValue(
                resource("v4-evaluation-with-unknown-condition.json"), ServerSideEvaluation.class);

        var conditions = evaluation.getRules().orElseThrow().get(0).getConditions();

        assertThat(conditions.get(0)).isInstanceOf(PercentageByContextCondition.class);
        assertThat(conditions.get(1))
                .isInstanceOfSatisfying(UnknownCondition.class,
                        unknown -> assertThat(unknown.getType()).hasValue("some-future-condition"));
    }

    @Test
    void shouldDeserializeEvaluationsResponseAsListOfEvaluations() throws Exception {
        var evaluations = objectMapper.readValue(
                resource("v4-evaluation-list.json"),
                new TypeReference<List<ServerSideEvaluation>>() {}
        );

        assertThat(evaluations).hasSize(2);

        assertThat(evaluations.get(0).getSlug()).isEqualTo("resolved-feature");
        assertThat(evaluations.get(0).getValue()).hasValue(false);
        assertThat(evaluations.get(0).getRules()).isEmpty();

        assertThat(evaluations.get(1).getSlug()).isEqualTo("deferred-feature");
        assertThat(evaluations.get(1).getValue()).isEmpty();

        var rules = evaluations.get(1).getRules().orElseThrow();
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getConditions().get(0)).isInstanceOf(PercentageByContextCondition.class);
    }

    @Test
    void shouldDeserializeEvaluationWithoutASlugRatherThanFailingTheResponse() throws Exception {
        // No property is required at parse time: a malformed flag is reported when it is evaluated, so
        // it costs only itself rather than every other flag in the response.
        var evaluation = objectMapper.readValue(
                resource("v4-evaluation-missing-slug.json"), ServerSideEvaluation.class);

        assertThat(evaluation.getSlug()).isNull();
        assertThat(evaluation.getValue()).hasValue(true);
    }

    @Test
    void shouldDeserializeEveryFlagWhenOneOfThemIsMissingItsSlug() throws Exception {
        var evaluations = objectMapper.readValue(
                resource("v4-evaluation-list-one-missing-slug.json"),
                new TypeReference<List<ServerSideEvaluation>>() {}
        );

        assertThat(evaluations).hasSize(2);
        assertThat(evaluations.get(0).getSlug()).isNull();
        assertThat(evaluations.get(1).getSlug()).isEqualTo("well-formed-feature");
    }

    @Test
    void shouldIgnoreExtraneousProperties() throws Exception {
        var evaluation = objectMapper.readValue(
                resource("v4-evaluation-with-extraneous-properties.json"), ServerSideEvaluation.class);

        assertThat(evaluation.getSlug()).isEqualTo("my-feature");

        var conditions = evaluation.getRules().orElseThrow().get(0).getConditions();
        assertThat(conditions.get(0))
                .isInstanceOfSatisfying(PercentageByContextCondition.class,
                        percentage -> assertThat(percentage.getPercentage()).hasValue(50));
    }

    @Test
    void shouldExposeConditionsAsImmutableLists() throws Exception {
        var evaluation = objectMapper.readValue(
                resource("v4-evaluation-deferred-to-client.json"), ServerSideEvaluation.class);

        var rules = evaluation.getRules().orElseThrow();

        assertThatThrownBy(() -> rules.clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> rules.get(0).getConditions().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
