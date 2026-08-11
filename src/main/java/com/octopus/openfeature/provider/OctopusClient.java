package com.octopus.openfeature.provider;

import com.fasterxml.jackson.core.JsonProcessingException;

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

    Boolean haveFeatureFlagsChanged(byte[] contentHash) throws IOException, InterruptedException {
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
        FeatureFlagCheckResponse checkResponse = OctopusObjectMapper.INSTANCE.readValue(httpResponse.body(), FeatureFlagCheckResponse.class);
        return !Arrays.equals(checkResponse.contentHash, contentHash);
    }

    EvaluationResponse getServerSideEvaluations() throws IOException, InterruptedException {
        URI evaluationsURI = getEvaluationsURI();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(evaluationsURI)
                .header("Authorization", String.format("Bearer %s", config.getClientIdentifier()))
                .header("X-Octopus-Client", buildOctopusClientHeaderValue())
                .build();
        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (httpResponse.statusCode() == StatusCodeNotFound) {
            logger.log(System.Logger.Level.WARNING, String.format("Failed to retrieve feature flags for client identifier %s from %s", config.getClientIdentifier(), evaluationsURI.toString()));
            return null;
        }
        Optional<String> contentHashHeader = httpResponse.headers().firstValue("ContentHash");
        if (contentHashHeader.isEmpty()) {
            logger.log(System.Logger.Level.WARNING, String.format("Feature flag response from %s did not contain expected ContentHash header", evaluationsURI.toString()));
            return null;
        }
        var evaluations = readEvaluations(httpResponse.body(), evaluationsURI);
        if (evaluations == null) {
            // Returning null leaves the cache on its previous context, or on the empty one, both of
            // which keep refetching. Storing a response with a usable content hash would not: the check
            // endpoint would report no change and the provider would never recover.
            return null;
        }
        return new EvaluationResponse(evaluations, Base64.getDecoder().decode(contentHashHeader.get()));
    }

    /**
     * Reads the evaluations one at a time, so a flag whose payload cannot be read costs only itself.
     *
     * <p>Deserializing the array in one call would abort on the first wrongly-typed field anywhere in it
     * — {@code "percentage": "lots"} on one flag would leave every flag in the response falling back to
     * the caller's default. Missing fields are already reported per flag, when the flag is evaluated;
     * this extends the same containment to fields of the wrong type, which cannot get that far because
     * they fail while being read.
     *
     * <p>Returns null when the response itself is unusable, which the caller treats as a failed fetch.
     */
    private List<ServerSideEvaluation> readEvaluations(String body, URI evaluationsURI) throws IOException {
        var root = OctopusObjectMapper.INSTANCE.readTree(body);
        if (root == null || !root.isArray()) {
            logger.log(System.Logger.Level.WARNING, String.format("Feature flag response content from %s was not a list of evaluations", evaluationsURI.toString()));
            return null;
        }

        var evaluations = new ArrayList<ServerSideEvaluation>();
        for (var element : root) {
            try {
                evaluations.add(OctopusObjectMapper.INSTANCE.treeToValue(element, ServerSideEvaluation.class));
            } catch (JsonProcessingException e) {
                // Left out of the response rather than guessed at: the flag resolves as not found, which
                // the caller sees as their default value, and the slug is logged so it can be traced.
                var slug = element.path("slug").isTextual() ? element.path("slug").asText() : "<unnamed>";
                logger.log(System.Logger.Level.WARNING, String.format(
                        "Could not read the evaluation for feature flag %s from %s, so it will resolve as not found: %s",
                        slug, evaluationsURI.toString(), e.getOriginalMessage()));
            }
        }
        return evaluations;
    }

    String buildOctopusClientHeaderValue() {
        var clientHeaderValueBuilder = new StringBuilder(this.config.getProductMetadata().getName());

        this.config.getProductMetadata().getVersion().ifPresent(s -> clientHeaderValueBuilder.append("/").append(s));

        clientHeaderValueBuilder.append(" openfeature-provider-java/").append(PROVIDER_VERSION);

        return clientHeaderValueBuilder.toString();
    }

    private URI getCheckURI() {
        try {
            return new URL(config.getServerUri().toURL(), "/api/feature-flags/check/v4/").toURI();
        } catch (MalformedURLException | URISyntaxException ignored) // we know this URL is well-formed
        {
        }
        return null;
    }

    private URI getEvaluationsURI() {
        try {
            return new URL(config.getServerUri().toURL(), "/api/feature-flags/evaluations/v4/").toURI();
        } catch (MalformedURLException | URISyntaxException ignored) // we know this URL is well-formed
        {
        }
        return null;
    }

    // This class needs to be static to allow deserialization
    private static class FeatureFlagCheckResponse {
        public byte[] contentHash;
    }
}
