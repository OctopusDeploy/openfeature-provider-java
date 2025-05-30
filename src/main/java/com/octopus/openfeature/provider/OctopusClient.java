package com.octopus.openfeature.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

class OctopusClient {

    private final OctopusConfiguration config;
    private static final System.Logger logger = System.getLogger(OctopusClient.class.getName());
    private static final int StatusCodeNotFound = 404;

    OctopusClient(OctopusConfiguration config){
        this.config = config;
    }
    
    Boolean haveFeatureTogglesChanged(byte[] contentHash)
    {
       if (contentHash.length == 0) { return true; }
       URI checkURI = getCheckURI();
        HttpClient client = HttpClient.newHttpClient();
       // TODO: check client is v3
        HttpRequest request = HttpRequest.newBuilder() 
                .GET()
                .uri(checkURI)
                .header("Authorization", String.format("Bearer %s", config.getClientIdentifier()))
                .build();
        try {
            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            FeatureToggleCheckResponse checkResponse = new ObjectMapper().readValue(httpResponse.body(), FeatureToggleCheckResponse.class);
            return !Arrays.equals(checkResponse.contentHash, contentHash);
        } catch (Exception e) {
            logger.log(System.Logger.Level.WARNING, String.format("Unable to query Octopus Feature Toggle service. URI: %s", checkURI.toString()), e);
            // Use cached toggles
            return false;
        }
    }
    
    FeatureToggles getFeatureToggleEvaluationManifest()
    {
        URI manifestURI = getManifestURI();
        HttpClient client = HttpClient.newHttpClient();
        // TODO: check client is v3
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(manifestURI)
                .header("Authorization", String.format("Bearer %s", config.getClientIdentifier()))
                .build();
        try {
            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == StatusCodeNotFound) {
               logger.log(System.Logger.Level.WARNING,String.format("Failed to retrieve feature toggles for client identifier %s from %s", config.getClientIdentifier(), manifestURI.toString())); 
               return null;
            }
            Optional<String> contentHashHeader = httpResponse.headers().firstValue("ContentHash");
            if (contentHashHeader.isEmpty()) {
                logger.log(System.Logger.Level.WARNING,String.format("Feature toggle response from %s did not contain expected ContentHash header", manifestURI.toString()));      
                return null;
            }
            List<FeatureToggleEvaluation> evaluations = new ObjectMapper().readValue(httpResponse.body(), new TypeReference<List<FeatureToggleEvaluation>>(){}); 
            return new FeatureToggles(evaluations, Base64.getDecoder().decode(contentHashHeader.get()));
        } catch (Exception e) {
            logger.log(System.Logger.Level.WARNING, "Unable to query Octopus Feature Toggle service", e);
            return null;
        }
    }
    
    private URI getCheckURI() {
        try {
            return new URL(config.getServerUri().toURL(), "/api/featuretoggles/check/v3/").toURI();
        } catch (MalformedURLException | URISyntaxException ignored) // we know this URL is well-formed
        { }
        return null;
    }
    
    private URI getManifestURI() {
        try {
            return new URL(config.getServerUri().toURL(), "/api/featuretoggles/v3/").toURI();
        } catch (MalformedURLException | URISyntaxException ignored) // we know this URL is well-formed
        { }
        return null;
    }
    
    private Boolean isSuccessStatusCode(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private class FeatureToggleCheckResponse {
       byte[] contentHash; 
    } 
}
