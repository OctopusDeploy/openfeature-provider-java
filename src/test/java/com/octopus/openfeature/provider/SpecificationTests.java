package com.octopus.openfeature.provider;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.openfeature.sdk.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificationTests {

    private static Server server;

    @BeforeAll
    static void startServer() {
        server = new Server();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @AfterEach
    void shutdownApi() {
        OpenFeatureAPI.getInstance().shutdown();
    }

    @ParameterizedTest(name = "[{0}] {1}")
    @MethodSource("fixtureTestCases")
    @Disabled("Requires either old endpoint to be used, or client rollout percentage to be implemented")
    void evaluate(String fileName, String description, String responseJson, FixtureCase testCase) {
        String token = server.configure(responseJson);
        OctopusConfiguration config = new OctopusConfiguration(token);
        config.setServerUri(URI.create(server.baseUrl()));

        OpenFeatureAPI api = OpenFeatureAPI.getInstance();
        api.setProviderAndWait(new OctopusProvider(config));
        Client client = api.getClient();

        EvaluationContext ctx = buildContext(testCase.configuration.context);
        FlagEvaluationDetails<Boolean> result = client.getBooleanDetails(
                testCase.configuration.slug,
                testCase.configuration.defaultValue,
                ctx
        );

        assertThat(result.getValue())
                .as("[%s] %s → value", fileName, description)
                .isEqualTo(testCase.expected.value);
        assertThat(result.getErrorCode())
                .as("[%s] %s → errorCode", fileName, description)
                .isEqualTo(mapErrorCode(testCase.expected.errorCode));
    }

    static Stream<Arguments> fixtureTestCases() throws IOException {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        List<Path> jsonFiles;
        try (Stream<Path> files = Files.list(Path.of("specification", "Fixtures"))) {
            jsonFiles = files
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .collect(Collectors.toList());
        }
        if (jsonFiles.isEmpty()) {
            throw new IllegalStateException(
                    "No fixture files found under 'specification/Fixtures/'. " +
                            "Ensure the git submodule is initialised: git submodule update --init");
        }
        return jsonFiles.stream().flatMap(path -> {
            try {
                String fileContent = Files.readString(path);
                Fixture fixture = mapper.readValue(fileContent, Fixture.class);
                String fileName = path.getFileName().toString();
                return Stream.of(fixture.cases)
                        .map(c -> Arguments.of(fileName, c.description, fixture.response, c));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static EvaluationContext buildContext(Map<String, String> context) {
        MutableContext ctx = new MutableContext();
        if (context != null) {
            context.forEach(ctx::add);
        }
        return ctx;
    }

    private static ErrorCode mapErrorCode(String code) {
        if (code == null) return null;
        switch (code) {
            case "FLAG_NOT_FOUND":
                return ErrorCode.FLAG_NOT_FOUND;
            case "PARSE_ERROR":
                return ErrorCode.PARSE_ERROR;
            case "TYPE_MISMATCH":
                return ErrorCode.TYPE_MISMATCH;
            case "TARGETING_KEY_MISSING":
                return ErrorCode.TARGETING_KEY_MISSING;
            case "PROVIDER_NOT_READY":
                return ErrorCode.PROVIDER_NOT_READY;
            case "INVALID_CONTEXT":
                return ErrorCode.INVALID_CONTEXT;
            case "PROVIDER_FATAL":
                return ErrorCode.PROVIDER_FATAL;
            case "GENERAL":
                return ErrorCode.GENERAL;
            default:
                throw new IllegalArgumentException("Unknown error code in fixture: " + code);
        }
    }

    static class Fixture {
        @JsonDeserialize(using = RawJsonDeserializer.class)
        public String response;
        public FixtureCase[] cases;
    }

    static class FixtureCase {
        public String description;
        public FixtureConfiguration configuration;
        public FixtureExpected expected;
    }

    static class FixtureConfiguration {
        public String slug;
        public boolean defaultValue;
        public Map<String, String> context;
    }

    static class FixtureExpected {
        public boolean value;
        public String errorCode;
    }

    static class RawJsonDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser jp, DeserializationContext dc) throws IOException {
            long begin = jp.currentLocation().getCharOffset();
            jp.skipChildren();
            long end = jp.currentLocation().getCharOffset();
            String json = jp.currentLocation().contentReference().getRawContent().toString();
            return json.substring((int) begin - 1, (int) end);
        }
    }

}
