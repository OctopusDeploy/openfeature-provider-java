package com.octopus.openfeature.provider.v4;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.octopus.openfeature.provider.TestObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises polymorphic JSON deserialization of the v4 evaluation response. Everything is
 * deserialized with the provider's own {@code OctopusObjectMapper} — the same mapper the client
 * uses in production — so discriminator matching, property binding and the absent-property
 * behaviour are all covered end to end.
 */
class ServerSideEvaluationDeserializationTests {

    private final ObjectMapper objectMapper = TestObjectMapper.INSTANCE;

    private InputStream resource(String name) {
        return getClass().getResourceAsStream(name);
    }

    @Test
    void shouldDeserializePercentageByContextConditionToConcreteType() throws Exception {
        var condition = objectMapper.readValue(
                resource("condition-percentage-by-context.json"), ClientSideCondition.class);

        assertThat(condition)
                .isInstanceOfSatisfying(PercentageByContextCondition.class,
                        percentage -> assertThat(percentage.getPercentage()).isEqualTo(50));
    }

    @Test
    void shouldDeserializeContextAttributeIsOneOfConditionToConcreteType() throws Exception {
        var condition = objectMapper.readValue(
                resource("condition-context-attribute-is-one-of.json"), ClientSideCondition.class);

        assertThat(condition)
                .isInstanceOfSatisfying(ContextAttributeIsOneOfCondition.class, isOneOf -> {
                    assertThat(isOneOf.getKey()).isEqualTo("user-id");
                    assertThat(isOneOf.getValues()).containsExactly("1234", "5678");
                });
    }

    @Test
    void shouldDeserializeContextAttributeIsNotOneOfConditionToConcreteType() throws Exception {
        var condition = objectMapper.readValue(
                resource("condition-context-attribute-is-not-one-of.json"), ClientSideCondition.class);

        assertThat(condition)
                .isInstanceOfSatisfying(ContextAttributeIsNotOneOfCondition.class, isNotOneOf -> {
                    assertThat(isNotOneOf.getKey()).isEqualTo("region");
                    assertThat(isNotOneOf.getValues()).containsExactly("us", "eu");
                });
    }

    @Test
    void shouldDeserializeMixedConditionListToConcreteTypes() throws Exception {
        var conditions = objectMapper.readValue(
                resource("condition-list-mixed.json"),
                new TypeReference<List<ClientSideCondition>>() {}
        );

        assertThat(conditions).hasExactlyElementsOfTypes(
                PercentageByContextCondition.class,
                ContextAttributeIsOneOfCondition.class,
                ContextAttributeIsNotOneOfCondition.class
        );
    }

    @Test
    void shouldDeserializeUnknownConditionTypeToUnknownConditionInsteadOfThrowing() throws Exception {
        var condition = objectMapper.readValue(
                resource("condition-unknown-type.json"), ClientSideCondition.class);

        assertThat(condition)
                .isInstanceOfSatisfying(UnknownCondition.class,
                        unknown -> assertThat(unknown.getType()).hasValue("not-a-real-condition"));
    }

    @Test
    void shouldDeserializeConditionWithoutTypeDiscriminatorToUnknownCondition() throws Exception {
        var condition = objectMapper.readValue(
                resource("condition-missing-type.json"), ClientSideCondition.class);

        assertThat(condition)
                .isInstanceOfSatisfying(UnknownCondition.class,
                        unknown -> assertThat(unknown.getType()).isEmpty());
    }

    @Test
    void shouldPreserveUnknownConditionAlongsideKnownConditionsWithoutFailingTheResponse() throws Exception {
        var evaluation = objectMapper.readValue(
                resource("evaluation-with-unknown-condition.json"), ServerSideEvaluation.class);

        var conditions = evaluation.getRules().orElseThrow().get(0).getConditions();

        assertThat(conditions.get(0)).isInstanceOf(PercentageByContextCondition.class);
        assertThat(conditions.get(1))
                .isInstanceOfSatisfying(UnknownCondition.class,
                        unknown -> assertThat(unknown.getType()).hasValue("some-future-condition"));
    }

    @Test
    void shouldDeserializeServerResolvedEvaluation() throws Exception {
        var evaluation = objectMapper.readValue(
                resource("evaluation-server-resolved.json"), ServerSideEvaluation.class);

        assertThat(evaluation.getSlug()).isEqualTo("my-feature");
        assertThat(evaluation.getValue()).hasValue(true);
        assertThat(evaluation.getReason()).hasValue("The flag is enabled for this environment.");
        assertThat(evaluation.getEvaluationKey()).isEmpty();
        assertThat(evaluation.getRules()).isEmpty();
    }

    @Test
    void shouldDeserializeEvaluationDeferredToTheClientWithPolymorphicConditions() throws Exception {
        var evaluation = objectMapper.readValue(
                resource("evaluation-deferred-to-client.json"), ServerSideEvaluation.class);

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
                        percentage -> assertThat(percentage.getPercentage()).isEqualTo(50));
        assertThat(rule.getConditions().get(1))
                .isInstanceOfSatisfying(ContextAttributeIsOneOfCondition.class, isOneOf -> {
                    assertThat(isOneOf.getKey()).isEqualTo("user-id");
                    assertThat(isOneOf.getValues()).containsExactly("1234", "5678");
                });
    }

    @Test
    void shouldDeserializeEvaluationsResponseAsListOfEvaluations() throws Exception {
        var evaluations = objectMapper.readValue(
                resource("evaluation-list.json"),
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
    void shouldFailDeserializationWhenSlugIsMissing() {
        assertThatThrownBy(() -> objectMapper.readValue(
                resource("evaluation-missing-slug.json"), ServerSideEvaluation.class))
                .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void shouldIgnoreExtraneousProperties() throws Exception {
        var evaluation = objectMapper.readValue(
                resource("evaluation-with-extraneous-properties.json"), ServerSideEvaluation.class);

        assertThat(evaluation.getSlug()).isEqualTo("my-feature");

        var conditions = evaluation.getRules().orElseThrow().get(0).getConditions();
        assertThat(conditions.get(0))
                .isInstanceOfSatisfying(PercentageByContextCondition.class,
                        percentage -> assertThat(percentage.getPercentage()).isEqualTo(50));
    }

    @Test
    void shouldExposeConditionsAsImmutableLists() throws Exception {
        var evaluation = objectMapper.readValue(
                resource("evaluation-deferred-to-client.json"), ServerSideEvaluation.class);

        var rules = evaluation.getRules().orElseThrow();

        assertThatThrownBy(() -> rules.clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> rules.get(0).getConditions().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
