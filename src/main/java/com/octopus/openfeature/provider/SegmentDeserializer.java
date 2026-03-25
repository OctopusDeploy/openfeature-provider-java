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
        String key = node.get("key").asText();
        String value = node.get("value").asText();
        return new AbstractMap.SimpleEntry<>(key, value);
    }
}
