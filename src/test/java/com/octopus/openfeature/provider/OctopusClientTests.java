package com.octopus.openfeature.provider;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

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
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("Could not load project.properties", e);
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
}
