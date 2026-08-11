package com.octopus.openfeature.provider;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Base type for a client-side rule condition, selected from the camelCase {@code type} discriminator
 * when deserializing a v4 evaluation response.
 *
 * <p>The conditions sit alongside the rest of the v4 types rather than in a {@code conditions}
 * sub-package as the .NET provider has them. Java package access is not hierarchical, so a
 * package-private condition there would be invisible to {@link ClientSideRule} in this package, and
 * following that layout would mean making every condition public — part of the library's supported
 * API, which is what keeping these types package-private is meant to avoid.
 */
@JsonDeserialize(using = ClientSideConditionDeserializer.class)
abstract class ClientSideCondition {

    /**
     * Whether this condition is met. A condition that did not arrive in a shape its type can evaluate
     * throws {@link dev.openfeature.sdk.exceptions.ParseError} rather than reading a value it was not
     * sent.
     */
    abstract boolean matches(ClientSideEvaluationContext context);
}
