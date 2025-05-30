package com.octopus.openfeature.provider;

import java.net.URI;
import java.time.Duration;

public class OctopusConfiguration {
    private final String clientIdentifier;
    private static final String DEFAULT_SERVER_URI = "https://features.octopus.com";
    private Duration cacheDuration = Duration.ofMinutes(1); 

    public OctopusConfiguration(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
    }

    public String getClientIdentifier() { return clientIdentifier; }
    
    public URI getServerUri() { return URI.create(DEFAULT_SERVER_URI); }

    public Duration getCacheDuration() {
        return cacheDuration;
    }
    
    public Duration setCacheDuration(Duration cacheDuration) {
        this.cacheDuration = cacheDuration;
        return this.cacheDuration;
    }
}
