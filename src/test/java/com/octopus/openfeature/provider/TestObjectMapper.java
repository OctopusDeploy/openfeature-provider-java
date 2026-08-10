package com.octopus.openfeature.provider;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Exposes the provider's package-private {@link OctopusObjectMapper} to tests in other packages, so
 * they can exercise deserialization with the same mapper the client uses in production rather than
 * a re-declared copy that could drift from it.
 *
 * <p>Test-only: this lives in the test sources and is never published.
 */
public final class TestObjectMapper {
    public static final ObjectMapper INSTANCE = OctopusObjectMapper.INSTANCE;

    private TestObjectMapper() {
    }
}
