package com.octopus.openfeature.provider;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class OctopusClientTests {

    private static final String PROVIDER_VERSION = loadProviderVersion();

    private static String loadProviderVersion() {
        try {
            var projectProperties = new Properties();
            try (var resourceStream = OctopusClient.class.getClassLoader().getResourceAsStream("project.properties"))
            {
                projectProperties.load(resourceStream);
            }

            var version = projectProperties.getProperty("version");
            assertThat(version).matches("\\d+.*"); // Ensure property filtering is working.
            return version;
        } catch (IOException e) {
            throw new RuntimeException("Could not load project.properties.", e);
        }
    }

    @Test
    void buildOctopusClientHeaderValue_withNameOnly_headerValueContainsProductNameAndProviderInformation() {
        var config = new OctopusConfiguration("test-id", new ProductMetadata("MyProduct"));
        var client = new OctopusClient(config);

        assertThat(client.buildOctopusClientHeaderValue())
                .isEqualTo("MyProduct openfeature-provider-java/" + PROVIDER_VERSION);
    }

    @Test
    void buildOctopusClientHeaderValue_withNameAndVersion_headerValueContainsProductAndProviderInformation() {
        var config = new OctopusConfiguration("test-id", new ProductMetadata("MyProduct", "2024.1.0"));
        var client = new OctopusClient(config);

        assertThat(client.buildOctopusClientHeaderValue())
                .isEqualTo("MyProduct/2024.1.0 openfeature-provider-java/" + PROVIDER_VERSION);
    }

    @Test
    void buildOctopusClientHeaderValue_withNameContainingUnsupportedChars_stripsCharsFromHeaderValue() {
        // Note: More character checking tests are in ProductMetadataTests.java

        var config = new OctopusConfiguration("test-id", new ProductMetadata("My Product"));
        var client = new OctopusClient(config);

        assertThat(client.buildOctopusClientHeaderValue())
                .isEqualTo("MyProduct openfeature-provider-java/" + PROVIDER_VERSION);
    }

    private static final String CHECK_PATH = "/api/feature-flags/check/v4/";
    private static final String EVALUATIONS_PATH = "/api/feature-flags/evaluations/v4/";
    private static final String CONTENT_HASH = Base64.getEncoder().encodeToString(new byte[]{0x01, 0x02});

    private WireMockServer wireMock;

    @BeforeEach
    void startServer() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterEach
    void stopServer() {
        wireMock.stop();
    }

    private OctopusClient clientForServer() {
        var config = new OctopusConfiguration("test-id", new ProductMetadata("MyProduct"));
        config.setServerUri(URI.create(wireMock.baseUrl()));
        return new OctopusClient(config);
    }

    private String requestedPaths() {
        return wireMock.getAllServeEvents().stream()
                .map(event -> event.getRequest().getUrl())
                .collect(Collectors.joining(", "));
    }

    @Test
    void haveFeatureFlagsChanged_requestsTheV4CheckEndpoint() throws Exception {
        wireMock.stubFor(get(anyUrl()).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"contentHash\":\"" + CONTENT_HASH + "\"}")));

        var haveChanged = clientForServer().haveFeatureFlagsChanged(new byte[]{0x03, 0x04});

        assertThat(requestedPaths()).isEqualTo(CHECK_PATH);
        assertThat(haveChanged).isTrue();
    }

    @Test
    void haveFeatureFlagsChanged_whenTheContentHashIsUnchanged_reportsNoChange() throws Exception {
        wireMock.stubFor(get(anyUrl()).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"contentHash\":\"" + CONTENT_HASH + "\"}")));

        var haveChanged = clientForServer().haveFeatureFlagsChanged(new byte[]{0x01, 0x02});

        assertThat(haveChanged).isFalse();
    }

    @Test
    void getServerSideEvaluations_requestsTheV4EvaluationsEndpoint() throws Exception {
        wireMock.stubFor(get(anyUrl()).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withHeader("ContentHash", CONTENT_HASH)
                .withBody("[{\"slug\":\"test-feature\",\"value\":true,\"reason\":\"The flag is enabled for this environment.\"}]")));

        var response = clientForServer().getServerSideEvaluations();

        assertThat(requestedPaths()).isEqualTo(EVALUATIONS_PATH);
        assertThat(response).isNotNull();
        assertThat(response.getContentHash()).isEqualTo(new byte[]{0x01, 0x02});
        assertThat(response.getEvaluations()).singleElement()
                .satisfies(evaluation -> assertThat(evaluation.getSlug()).isEqualTo("test-feature"));
    }
}
