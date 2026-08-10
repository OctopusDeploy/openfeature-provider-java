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
 *
 * <p>The conditions sit alongside the rest of the v4 types rather than in a {@code conditions}
 * sub-package as the .NET provider has them. Java package access is not hierarchical, so a
 * package-private condition there would be invisible to {@link ClientSideRule} in this package, and
 * following that layout would mean making every condition public — part of the library's supported
 * API, which is what keeping these types package-private is meant to avoid.
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
