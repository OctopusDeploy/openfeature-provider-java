package com.octopus.openfeature.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FlagEvaluationDetails;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.OpenFeatureAPI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtureTestCases")
    void evaluate(String description, String responseJson, FixtureCase testCase) {
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
            .as(description + " → value")
            .isEqualTo(testCase.expected.value);
        assertThat(result.getErrorCode())
            .as(description + " → errorCode")
            .isEqualTo(mapErrorCode(testCase.expected.errorCode));
    }

    static Stream<Arguments> fixtureTestCases() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
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
                Fixture fixture = mapper.readValue(path.toFile(), Fixture.class);
                String responseJson = mapper.writeValueAsString(fixture.response);
                return Stream.of(fixture.cases)
                    .map(c -> Arguments.of(c.description, responseJson, c));
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
            case "FLAG_NOT_FOUND":       return ErrorCode.FLAG_NOT_FOUND;
            case "PARSE_ERROR":          return ErrorCode.PARSE_ERROR;
            case "TYPE_MISMATCH":        return ErrorCode.TYPE_MISMATCH;
            case "TARGETING_KEY_MISSING": return ErrorCode.TARGETING_KEY_MISSING;
            case "PROVIDER_NOT_READY":   return ErrorCode.PROVIDER_NOT_READY;
            case "INVALID_CONTEXT":      return ErrorCode.INVALID_CONTEXT;
            case "PROVIDER_FATAL":       return ErrorCode.PROVIDER_FATAL;
            case "GENERAL":              return ErrorCode.GENERAL;
            default: throw new IllegalArgumentException("Unknown error code in fixture: " + code);
        }
    }

    // ---- Fixture model classes ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Fixture {
        public JsonNode response;
        public FixtureCase[] cases;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class FixtureCase {
        public String description;
        public FixtureConfiguration configuration;
        public FixtureExpected expected;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class FixtureConfiguration {
        public String slug;
        public boolean defaultValue;
        public Map<String, String> context;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class FixtureExpected {
        public boolean value;
        public String errorCode;
    }
}
