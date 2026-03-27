package com.octopus.openfeature.provider;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class OctopusConfigurationTests {

    @Test
    void defaultServerUriIsOctopusCloud() {
        var config = new OctopusConfiguration("test-client");
        assertThat(config.getServerUri()).isEqualTo(URI.create("https://features.octopus.com"));
    }

    @Test
    void serverUriCanBeOverridden() {
        var config = new OctopusConfiguration("test-client");
        var customUri = URI.create("http://localhost:8080");
        config.setServerUri(customUri);
        assertThat(config.getServerUri()).isEqualTo(customUri);
    }
}
