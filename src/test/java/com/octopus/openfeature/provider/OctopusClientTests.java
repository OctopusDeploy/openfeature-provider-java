package com.octopus.openfeature.provider;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class OctopusClientTests {

    private static final String PROVIDER_VERSION = loadProviderVersion();

    private static String loadProviderVersion() {
        try {
            var props = new Properties();
            props.load(OctopusClient.class.getClassLoader().getResourceAsStream("project.properties"));

            var version = props.getProperty("version");
            assertThat(version).matches("\\d+.*"); // Ensure property filtering is working.
            return version;
        } catch (IOException e) {
            throw new RuntimeException("Could not load project.properties", e);
        }
    }

    @Test
    void buildOctopusClientHeader_withNameOnly_headerContainsProductNameAndProviderInformation() {
        var config = new OctopusConfiguration("test-id", new ProductMetadata("MyProduct"));
        var client = new OctopusClient(config);

        assertThat(client.buildOctopusClientHeader())
                .isEqualTo("MyProduct openfeature-provider-java/" + PROVIDER_VERSION);
    }

    @Test
    void buildOctopusClientHeader_withNameAndVersion_headerContainsProductAndProviderInformation() {
        var config = new OctopusConfiguration("test-id", new ProductMetadata("MyProduct", "2024.1.0"));
        var client = new OctopusClient(config);

        assertThat(client.buildOctopusClientHeader())
                .isEqualTo("MyProduct/2024.1.0 openfeature-provider-java/" + PROVIDER_VERSION);
    }

    @Test
    void buildOctopusClientHeader_withNameContainingUnsupportedChars_stripsCharsFromHeader() {
        // Note: More character checking tests are in ProductMetadataTests.java

        var config = new OctopusConfiguration("test-id", new ProductMetadata("My Product"));
        var client = new OctopusClient(config);

        assertThat(client.buildOctopusClientHeader())
                .isEqualTo("MyProduct openfeature-provider-java/" + PROVIDER_VERSION);
    }
}
