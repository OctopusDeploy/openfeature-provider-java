package com.octopus.openfeature.provider;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OctopusFeatureConfigurationTests {

    @Test
    void constructor_whenNullClientIdentifierProvided_throwsNullPointerException() {
        assertThatThrownBy(() -> new OctopusFeatureConfiguration(null, new ProductMetadata("TestClient")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_whenNullProductMetadataProvided_throwsNullPointerException() {
        assertThatThrownBy(() -> new OctopusFeatureConfiguration("test-client", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void defaultServerUriIsOctopusCloud() {
        var config = new OctopusFeatureConfiguration("test-client", new ProductMetadata("TestClient"));
        assertThat(config.getServerUri()).isEqualTo(URI.create("https://features.octopus.com"));
    }

    @Test
    void serverUriCanBeOverridden() {
        var config = new OctopusFeatureConfiguration("test-client", new ProductMetadata("TestClient"));
        var customUri = URI.create("http://localhost:8080");
        config.setServerUri(customUri);
        assertThat(config.getServerUri()).isEqualTo(customUri);
    }
}
