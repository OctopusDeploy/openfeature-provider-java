package com.octopus.openfeature.provider;

import java.net.URI;
import java.time.Duration;

public class OctopusConfiguration {
    private final String clientIdentifier;
    private static final URI DEFAULT_SERVER_URI = URI.create("https://features.octopus.com");
    private URI serverUri = DEFAULT_SERVER_URI;
    private Duration cacheDuration = Duration.ofMinutes(1);

    public OctopusConfiguration(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
    }

    public String getClientIdentifier() { return clientIdentifier; }

    public URI getServerUri() { return serverUri; }

    // Package-private: visible to tests in same package, not to library consumers.
    void setServerUri(URI serverUri) { this.serverUri = serverUri; }

    public Duration getCacheDuration() {
        return cacheDuration;
    }

    public Duration setCacheDuration(Duration cacheDuration) {
        this.cacheDuration = cacheDuration;
        return this.cacheDuration;
    }
}
