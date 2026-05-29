package com.octopus.openfeature.provider;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

class OctopusClient {

    private final OctopusConfiguration config;
    private static final System.Logger logger = System.getLogger(OctopusClient.class.getName());
    private static final int StatusCodeNotFound = 404;
    private static final String PROVIDER_VERSION = loadProviderVersion();

    private static String loadProviderVersion() {
        try {
            var projectProperties = new Properties();
            try (var resourceStream = OctopusClient.class.getClassLoader().getResourceAsStream("project.properties"))
            {
                if(resourceStream == null) {
                    logger.log(System.Logger.Level.WARNING, "Unable to load project properties to determine provider version.");
                    return null;
                }

                projectProperties.load(resourceStream);
            }

            return projectProperties.getProperty("version");
        } catch (IOException e) {
            logger.log(System.Logger.Level.WARNING, "Unable to load project properties to determine provider version.", e);
            return null;
        }
    }

    OctopusClient(OctopusConfiguration config) {
        this.config = config;
    }

    Boolean haveFeatureTogglesChanged(byte[] contentHash) throws IOException, InterruptedException {
        if (contentHash.length == 0) {
            return true;
        }
        URI checkURI = getCheckURI();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(checkURI)
                .header("Authorization", String.format("Bearer %s", config.getClientIdentifier()))
                .header("X-Octopus-Client", buildOctopusClientHeaderValue())
                .build();
        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        FeatureToggleCheckResponse checkResponse = OctopusObjectMapper.INSTANCE.readValue(httpResponse.body(), FeatureToggleCheckResponse.class);
        return !Arrays.equals(checkResponse.contentHash, contentHash);
    }

    FeatureToggles getFeatureToggleEvaluationManifest() throws IOException, InterruptedException {
        URI manifestURI = getManifestURI();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(manifestURI)
                .header("Authorization", String.format("Bearer %s", config.getClientIdentifier()))
                .header("X-Octopus-Client", buildOctopusClientHeaderValue())
                .build();
        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (httpResponse.statusCode() == StatusCodeNotFound) {
            logger.log(System.Logger.Level.WARNING, String.format("Failed to retrieve feature toggles for client identifier %s from %s", config.getClientIdentifier(), manifestURI.toString()));
            return null;
        }
        Optional<String> contentHashHeader = httpResponse.headers().firstValue("ContentHash");
        if (contentHashHeader.isEmpty()) {
            logger.log(System.Logger.Level.WARNING, String.format("Feature toggle response from %s did not contain expected ContentHash header", manifestURI.toString()));
            return null;
        }
        var evaluations = OctopusObjectMapper.INSTANCE.readValue(httpResponse.body(), new TypeReference<List<FeatureToggleEvaluation>>() {});
        return new FeatureToggles(evaluations, Base64.getDecoder().decode(contentHashHeader.get()));
    }

    String buildOctopusClientHeaderValue() {
        var clientHeaderValueBuilder = new StringBuilder(this.config.getProductMetadata().getName());

        this.config.getProductMetadata().getVersion().ifPresent(s -> clientHeaderValueBuilder.append("/").append(s));

        clientHeaderValueBuilder.append(" openfeature-provider-java/").append(PROVIDER_VERSION);

        return clientHeaderValueBuilder.toString();
    }

    private URI getCheckURI() {
        try {
            return new URL(config.getServerUri().toURL(), "/api/featuretoggles/check/v3/").toURI();
        } catch (MalformedURLException | URISyntaxException ignored) // we know this URL is well-formed
        {
        }
        return null;
    }

    private URI getManifestURI() {
        try {
            return new URL(config.getServerUri().toURL(), "/api/toggles/evaluations/v3/").toURI();
        } catch (MalformedURLException | URISyntaxException ignored) // we know this URL is well-formed
        {
        }
        return null;
    }

    // This class needs to be static to allow deserialization
    private static class FeatureToggleCheckResponse {
        public byte[] contentHash;
    }
}
