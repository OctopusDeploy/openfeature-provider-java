package com.octopus.openfeature.provider;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class OctopusContextProviderTests {

    static class FakeClient extends OctopusClient {
        private volatile FeatureToggles toggles;

        FakeClient(FeatureToggles toggles) {
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

    private OctopusConfiguration fastConfig() {
        var config = new OctopusConfiguration("token");
        config.setCacheDuration(Duration.ofMillis(100));
        return config;
    }

    @Test
    void whenInitialized_RefreshesCacheAfterCacheDurationExpires() throws InterruptedException {
        byte[] initialHash = {0x01, 0x02, 0x03, 0x04};
        byte[] updatedHash = {0x01, 0x02, 0x03, 0x05};

        var client = new FakeClient(new FeatureToggles(
            List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 100)),
            initialHash
        ));
        var provider = new OctopusContextProvider(fastConfig(), client);
        provider.initialize();

        try {
            assertThat(provider.getOctopusContext().getContentHash()).isEqualTo(initialHash);
            assertThat(provider.getOctopusContext().evaluate("test-feature", false, null).getValue()).isTrue();

            client.changeToggles(new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", false, "evaluation-key", Collections.emptyList(), 100)),
                updatedHash
            ));
            Thread.sleep(500);

            assertThat(provider.getOctopusContext().getContentHash()).isEqualTo(updatedHash);
            assertThat(provider.getOctopusContext().evaluate("test-feature", false, null).getValue()).isFalse();
        } finally {
            provider.shutdown();
        }
    }

    @Test
    void whenInitialized_AndRefreshFails_RetainsExistingContextAndLogsError() throws InterruptedException {
        byte[] contentHash = {0x01, 0x02, 0x03, 0x04};

        var client = new FakeClient(new FeatureToggles(
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

        var provider = new OctopusContextProvider(fastConfig(), client);
        try {
            provider.initialize();

            client.changeToggles(null);
            Thread.sleep(500);

            assertThat(provider.getOctopusContext().getContentHash()).isEqualTo(contentHash);
            assertThat(provider.getOctopusContext().evaluate("test-feature", false, null).getValue()).isTrue();
            assertThat(logMessages).anyMatch(m -> m.startsWith("Failed to retrieve updated feature manifest"));
        } finally {
            julLogger.removeHandler(handler);
            provider.shutdown();
        }
    }
}
