package com.octopus.openfeature.provider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class OctopusContextProviderTests {

    static class MockOctopusFeatureClient extends OctopusClient {

        private volatile FeatureToggles toggles;

        MockOctopusFeatureClient(FeatureToggles toggles) {
            super(null);
            this.toggles = toggles;
        }

        void changeToggles(FeatureToggles toggles) {
            this.toggles = toggles;
        }

        @Override
        Boolean haveFeatureTogglesChanged(byte[] contentHash) {
            return true;
        }

        @Override
        FeatureToggles getFeatureToggleEvaluationManifest() {
            return toggles;
        }
    }

    private final OctopusConfiguration configuration = configure();

    private static OctopusConfiguration configure() {
        var config = new OctopusConfiguration("token");
        config.setCacheDuration(Duration.ofMillis(100));
        return config;
    }

    @Test
    void whenInitialized_RefreshesCacheAfterCacheDurationExpires() throws InterruptedException {

        byte[] initialHash = {0x01, 0x02, 0x03, 0x04};
        byte[] updatedHash = {0x01, 0x02, 0x03, 0x05};

        var client = new MockOctopusFeatureClient(new FeatureToggles(
            List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 100)),
            initialHash
        ));

        var provider = new OctopusContextProvider(configuration, client);
        provider.initialize();

        try {
            // Validate the initial state
            assertThat(provider.getOctopusContext().getContentHash()).isEqualTo(initialHash);
            assertThat(provider.getOctopusContext().evaluate("test-feature", false, null).getValue()).isTrue();

            // Simulate a change in the available feature toggles
            client.changeToggles(new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", false, "evaluation-key", Collections.emptyList(), 100)),
                updatedHash
            ));

            // Wait for the cache to expire
            Thread.sleep(500);

            // Validate the updated toggles are available
            assertThat(provider.getOctopusContext().getContentHash()).isEqualTo(updatedHash);
            assertThat(provider.getOctopusContext().evaluate("test-feature", true, null).getValue()).isFalse();

        } finally {
            provider.shutdown();
        }
    }

    @Test
    void whenInitialized_AndRefreshFails_RetainsExistingContextAndLogsError() throws InterruptedException {

        byte[] contentHash = {0x01, 0x02, 0x03, 0x04};

        var client = new MockOctopusFeatureClient(new FeatureToggles(
            List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 100)),
            contentHash
        ));

        var logMessages = new ArrayList<String>();
        var julLogger = Logger.getLogger(OctopusClient.class.getName());
        var handler = new Handler() {
            @Override public void publish(LogRecord record) { logMessages.add(record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        julLogger.addHandler(handler);

        var provider = new OctopusContextProvider(configuration, client);

        try {
            provider.initialize();

            // Simulate a failed fetch
            client.changeToggles(null);

            // Wait for the cache to expire
            Thread.sleep(500);

            // Validate that the existing context is retained and an error was logged
            assertThat(provider.getOctopusContext().getContentHash()).isEqualTo(contentHash);
            assertThat(provider.getOctopusContext().evaluate("test-feature", false, null).getValue()).isTrue();
            assertThat(logMessages).anyMatch(m -> m.startsWith("Failed to retrieve updated feature manifest"));

        } finally {
            julLogger.removeHandler(handler);
            provider.shutdown();
        }
    }
}
