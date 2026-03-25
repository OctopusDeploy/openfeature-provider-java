package com.octopus.openfeature.provider;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;

class SegmentDeserializer extends JsonDeserializer<Map.Entry<String, String>> {
    @Override
    public Map.Entry<String, String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || !node.isObject()) {
            ctxt.reportInputMismatch(
                Map.Entry.class,
                "Expected JSON object for Segment but got: %s",
                node
            );
            return null;
        }

        JsonNode keyNode = node.get("key");
        if (keyNode == null || keyNode.isNull() || !keyNode.isTextual()) {
            ctxt.reportInputMismatch(
                Map.Entry.class,
                "Expected non-null string 'key' field but got: %s",
                keyNode
            );
            return null;
        }

        JsonNode valueNode = node.get("value");
        if (valueNode == null || valueNode.isNull() || !valueNode.isTextual()) {
            ctxt.reportInputMismatch(
                Map.Entry.class,
                "Expected non-null string 'value' field but got: %s",
                valueNode
            );
            return null;
        }

        String key = keyNode.asText();
        String value = valueNode.asText();

        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }
}
