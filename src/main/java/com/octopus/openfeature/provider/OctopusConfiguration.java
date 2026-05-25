package com.octopus.openfeature.provider;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

public class OctopusConfiguration {
    private final String clientIdentifier;
    private final ProductMetadata productMetadata;

    private static final URI DEFAULT_SERVER_URI = URI.create("https://features.octopus.com");
    private URI serverUri = DEFAULT_SERVER_URI;
    private Duration cacheDuration = Duration.ofMinutes(1);

    public OctopusConfiguration(String clientIdentifier, ProductMetadata productMetadata) {
        this.clientIdentifier = Objects.requireNonNull(clientIdentifier);
        this.productMetadata = Objects.requireNonNull(productMetadata);
    }

    public String getClientIdentifier() { return clientIdentifier; }

    public ProductMetadata getProductMetadata() { return productMetadata; }

    public URI getServerUri() { return serverUri; }

    // Note: package-private by default. Visible to tests in same package, but not to library consumers.
    void setServerUri(URI serverUri) { this.serverUri = serverUri; }

    public Duration getCacheDuration() {
        return cacheDuration;
    }

    public Duration setCacheDuration(Duration cacheDuration) {
        this.cacheDuration = cacheDuration;
        return this.cacheDuration;
    }
}
