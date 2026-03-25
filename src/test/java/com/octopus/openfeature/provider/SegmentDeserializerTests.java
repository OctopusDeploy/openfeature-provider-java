package com.octopus.openfeature.provider;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SegmentDeserializerTests {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new SimpleModule().addDeserializer(
                    Map.Entry.class,
                    new SegmentDeserializer()
            ));

    @Test
    void shouldDeserializeSegmentWithKeyAndValue() throws Exception {
        Map.Entry<?, ?> result = objectMapper.readValue(
                "{\"key\":\"license-type\",\"value\":\"free\"}",
                Map.Entry.class
        );

        assertThat(result.getKey()).isEqualTo("license-type");
        assertThat(result.getValue()).isEqualTo("free");
    }

    @Test
    void shouldIgnoreExtraFields() throws Exception {
        Map.Entry<?, ?> result = objectMapper.readValue(
                "{\"key\":\"k\",\"value\":\"v\",\"extra\":\"ignored\"}",
                Map.Entry.class
        );

        assertThat(result.getKey()).isEqualTo("k");
        assertThat(result.getValue()).isEqualTo("v");
    }

    @Test
    void shouldRejectMissingKey() {
        assertThatThrownBy(() -> objectMapper.readValue("{\"value\":\"v\"}", Map.Entry.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("key");
    }

    @Test
    void shouldRejectNullKey() {
        assertThatThrownBy(() -> objectMapper.readValue("{\"key\":null,\"value\":\"v\"}", Map.Entry.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("key");
    }

    @Test
    void shouldRejectNonStringKey() {
        assertThatThrownBy(() -> objectMapper.readValue("{\"key\":42,\"value\":\"v\"}", Map.Entry.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("key");
    }

    @Test
    void shouldRejectMissingValue() {
        assertThatThrownBy(() -> objectMapper.readValue("{\"key\":\"k\"}", Map.Entry.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("value");
    }

    @Test
    void shouldRejectNullValue() {
        assertThatThrownBy(() -> objectMapper.readValue("{\"key\":\"k\",\"value\":null}", Map.Entry.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("value");
    }

    @Test
    void shouldRejectNonStringValue() {
        assertThatThrownBy(() -> objectMapper.readValue("{\"key\":\"k\",\"value\":42}", Map.Entry.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("value");
    }

    @Test
    void shouldRejectArrayInput() {
        assertThatThrownBy(() -> objectMapper.readValue("[\"k\",\"v\"]", Map.Entry.class))
                .isInstanceOf(JsonMappingException.class);
    }

    @Test
    void shouldRejectScalarInput() {
        assertThatThrownBy(() -> objectMapper.readValue("\"just-a-string\"", Map.Entry.class))
                .isInstanceOf(JsonMappingException.class);
    }
}
