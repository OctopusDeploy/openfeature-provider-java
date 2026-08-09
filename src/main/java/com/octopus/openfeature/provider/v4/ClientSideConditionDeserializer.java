package com.octopus.openfeature.provider.v4;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Map;

/**
 * Selects the concrete {@link ClientSideCondition} from the camelCase {@code type} discriminator. An
 * unrecognised discriminator deserializes to {@link UnknownCondition} rather than throwing, so a
 * condition type introduced by a newer server degrades safely on an older client.
 *
 * <p>Written by hand rather than driven by {@code @JsonTypeInfo}, which cannot distinguish a
 * discriminator that is not a string — Jackson coerces {@code "type": 123} to {@code "123"} and
 * treats it as merely unrecognised. Here a non-string (or absent) discriminator yields an
 * {@link UnknownCondition} carrying no type, which fails evaluation as the malformed response it is.
 *
 * <p>The provider only ever reads these conditions, so serialization is left to Jackson's defaults.
 */
final class ClientSideConditionDeserializer extends JsonDeserializer<ClientSideCondition> {

    private static final String DISCRIMINATOR = "type";

    @Override
    public ClientSideCondition deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

        JsonNode discriminator = findDiscriminator(node);
        String type = discriminator != null && discriminator.isTextual() ? discriminator.textValue() : null;

        // Deserializing the concrete type targets that type directly, so this deserializer — registered
        // on the base type only — is not re-entered.
        if (type == null) {
            return new UnknownCondition(null);
        }

        switch (type) {
            case ConditionTypeNames.PERCENTAGE_BY_CONTEXT:
                return codec.treeToValue(node, PercentageByContextCondition.class);
            case ConditionTypeNames.CONTEXT_ATTRIBUTE_IS_ONE_OF:
                return codec.treeToValue(node, ContextAttributeIsOneOfCondition.class);
            case ConditionTypeNames.CONTEXT_ATTRIBUTE_IS_NOT_ONE_OF:
                return codec.treeToValue(node, ContextAttributeIsNotOneOfCondition.class);
            default:
                return new UnknownCondition(type);
        }
    }

    /**
     * The discriminator, matched without regard to case as the provider's mapper matches every other
     * property name.
     */
    private static JsonNode findDiscriminator(JsonNode node) {
        var fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (DISCRIMINATOR.equalsIgnoreCase(field.getKey())) {
                return field.getValue();
            }
        }
        return null;
    }
}
