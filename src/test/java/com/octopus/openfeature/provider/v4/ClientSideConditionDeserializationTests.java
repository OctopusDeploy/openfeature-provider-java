package com.octopus.openfeature.provider.v4;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.openfeature.provider.TestObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Polymorphic deserialization of a single condition, using the provider's own
 * {@code OctopusObjectMapper} as the client does in production. Whole responses are covered by
 * {@link ServerSideEvaluationDeserializationTests}.
 */
class ClientSideConditionDeserializationTests {

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
                        percentage -> assertThat(percentage.getPercentage()).hasValue(50));
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

    // A discriminator that is not a string is a response no server sends, so it is kept distinct from
    // a merely unrecognised one: it carries no type, and so fails evaluation rather than degrading
    // quietly. This is the distinction Jackson's own polymorphic handling cannot express — it coerces
    // any scalar type id to a string, which would make 123 indistinguishable from "123" — and the
    // reason ClientSideConditionDeserializer is written by hand rather than driven by @JsonTypeInfo.
    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "{ 'type': 123, 'percentage': 50 }",
            "{ 'type': true, 'percentage': 50 }",
            "{ 'type': null, 'percentage': 50 }",
            "{ 'type': {}, 'percentage': 50 }",
            "{ 'type': [], 'percentage': 50 }"
    })
    void shouldDeserializeUnusableDiscriminatorToUnknownConditionCarryingNoType(String conditionJson) throws Exception {
        var condition = objectMapper.readValue(Contexts.json(conditionJson), ClientSideCondition.class);

        assertThat(condition)
                .isInstanceOfSatisfying(UnknownCondition.class,
                        unknown -> assertThat(unknown.getType()).isEmpty());
    }
}
