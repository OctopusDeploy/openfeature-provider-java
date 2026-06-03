package com.octopus.openfeature.provider;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OctopusProviderTests {

    private Client client;

    @BeforeEach
    void setup() throws Exception {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-a", true, "key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var provider = new OctopusProvider(new OctopusContextProvider(new OctopusContext(toggles)));
        OpenFeatureAPI.getInstance().setProviderAndWait(provider);
        client = OpenFeatureAPI.getInstance().getClient();
    }

    @AfterEach
    void teardown() {
        OpenFeatureAPI.getInstance().shutdown();
    }

    @Test
    void givenAKnownFlag_whenRequestedAsString_returnsTypeMismatch() {
        assertThat(client.getStringDetails("feature-a", "default").getErrorCode())
                .isEqualTo(ErrorCode.TYPE_MISMATCH);
    }

    @Test
    void givenAnUnknownFlag_whenRequestedAsString_returnsFlagNotFound() {
        assertThat(client.getStringDetails("nonexistent", "default").getErrorCode())
                .isEqualTo(ErrorCode.FLAG_NOT_FOUND);
    }

    @Test
    void givenAKnownFlag_whenRequestedAsInteger_returnsTypeMismatch() {
        assertThat(client.getIntegerDetails("feature-a", 0).getErrorCode())
                .isEqualTo(ErrorCode.TYPE_MISMATCH);
    }

    @Test
    void givenAnUnknownFlag_whenRequestedAsInteger_returnsFlagNotFound() {
        assertThat(client.getIntegerDetails("nonexistent", 0).getErrorCode())
                .isEqualTo(ErrorCode.FLAG_NOT_FOUND);
    }

    @Test
    void givenAKnownFlag_whenRequestedAsDouble_returnsTypeMismatch() {
        assertThat(client.getDoubleDetails("feature-a", 0.0).getErrorCode())
                .isEqualTo(ErrorCode.TYPE_MISMATCH);
    }

    @Test
    void givenAnUnknownFlag_whenRequestedAsDouble_returnsFlagNotFound() {
        assertThat(client.getDoubleDetails("nonexistent", 0.0).getErrorCode())
                .isEqualTo(ErrorCode.FLAG_NOT_FOUND);
    }

    @Test
    void givenAKnownFlag_whenRequestedAsObject_returnsTypeMismatch() {
        assertThat(client.getObjectDetails("feature-a", new Value()).getErrorCode())
                .isEqualTo(ErrorCode.TYPE_MISMATCH);
    }

    @Test
    void givenAnUnknownFlag_whenRequestedAsObject_returnsFlagNotFound() {
        assertThat(client.getObjectDetails("nonexistent", new Value()).getErrorCode())
                .isEqualTo(ErrorCode.FLAG_NOT_FOUND);
    }
}
