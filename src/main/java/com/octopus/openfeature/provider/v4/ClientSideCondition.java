package com.octopus.openfeature.provider.v4;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base type for a client-side rule condition, selected from the camelCase {@code type} discriminator
 * when deserializing a v4 evaluation response. These types model the wire shape only.
 *
 * <p>A discriminator this version of the provider does not recognise — or an absent one —
 * deserializes to {@link UnknownCondition} rather than failing, so a condition type
 * introduced by a newer server degrades safely on an older client.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = UnknownCondition.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PercentageByContextCondition.class, name = ConditionTypeNames.PERCENTAGE_BY_CONTEXT),
        @JsonSubTypes.Type(value = ContextAttributeIsOneOfCondition.class, name = ConditionTypeNames.CONTEXT_ATTRIBUTE_IS_ONE_OF),
        @JsonSubTypes.Type(value = ContextAttributeIsNotOneOfCondition.class, name = ConditionTypeNames.CONTEXT_ATTRIBUTE_IS_NOT_ONE_OF)
})
abstract class ClientSideCondition {
}
