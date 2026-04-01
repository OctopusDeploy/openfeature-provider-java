package com.octopus.openfeature.provider;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.Base64;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Fake HTTP server for specification tests.
 *
 * Each call to {@link #configure(String)} registers a stub for a unique Bearer token
 * and returns that token as the client identifier. Stubs accumulate over the server's
 * lifetime (one per test case), which is harmless since each token is unique.
 *
 * Note: parallel test execution is not supported because SpecificationTests uses
 * the OpenFeatureAPI singleton.
 */
class Server {

    // A fixed hash is safe here because each test shuts down the provider via OpenFeatureAPI.shutdown()
    // before the background refresh thread can poll the check endpoint and compare hashes.
    private static final String CONTENT_HASH = Base64.getEncoder().encodeToString(new byte[]{0x01});
    private final WireMockServer wireMock;

    Server() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        // Fallback: return 401 for any request that does not match a registered token.
        wireMock.stubFor(any(anyUrl())
            .atPriority(100)
            .willReturn(aResponse().withStatus(401)));
    }

    /**
     * Registers the given JSON as the response body for a new unique client token.
     *
     * @param responseJson the JSON array that the toggle API would return
     * @return the client identifier (Bearer token) to use in OctopusConfiguration
     */
    String configure(String responseJson) {
        String token = UUID.randomUUID().toString();
        wireMock.stubFor(get(urlPathEqualTo("/api/toggles/evaluations/v3/"))
            .withHeader("Authorization", equalTo("Bearer " + token))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withHeader("ContentHash", CONTENT_HASH)
                .withBody(responseJson)));
        return token;
    }

    String baseUrl() {
        return wireMock.baseUrl();
    }

    void stop() {
        wireMock.stop();
    }
}
