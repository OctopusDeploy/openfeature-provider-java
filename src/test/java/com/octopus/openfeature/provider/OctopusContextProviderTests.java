package com.octopus.openfeature.provider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class OctopusContextProviderTests {

    static class MockOctopusFeatureClient extends OctopusClient {

        private volatile EvaluationResponse evaluationResponse;

        MockOctopusFeatureClient(EvaluationResponse evaluationResponse) {
            super(null);
            this.evaluationResponse = evaluationResponse;
        }

        void changeEvaluations(EvaluationResponse evaluationResponse) {
            this.evaluationResponse = evaluationResponse;
        }

        @Override
        Boolean haveFeatureFlagsChanged(byte[] contentHash) {
            return true;
        }

        @Override
        EvaluationResponse getServerSideEvaluations() {
            return evaluationResponse;
        }
    }

    private static EvaluationResponse response(boolean value, byte[] contentHash) {
        return new EvaluationResponse(
                List.of(new ServerSideEvaluation("test-feature", value,
                        value ? "The flag is enabled for this environment." : "The flag is disabled for this environment.",
                        null, null)),
                contentHash);
    }

    private final OctopusConfiguration configuration = configure();

    private static OctopusConfiguration configure() {
        var config = new OctopusConfiguration("token", new ProductMetadata("TestClient"));
        config.setCacheDuration(Duration.ofMillis(100));
        return config;
    }

    @Test
    void whenInitialized_RefreshesCacheAfterCacheDurationExpires() throws InterruptedException {

        byte[] initialHash = {0x01, 0x02, 0x03, 0x04};
        byte[] updatedHash = {0x01, 0x02, 0x03, 0x05};

        var client = new MockOctopusFeatureClient(response(true, initialHash));

        var provider = new OctopusContextProvider(configuration, client);
        provider.initialize();

        try {
            // Validate the initial state
            assertThat(provider.getOctopusContext().getContentHash()).isEqualTo(initialHash);
            assertThat(provider.getOctopusContext().evaluate("test-feature", null).getValue()).isTrue();

            // Simulate a change in the available feature toggles
            client.changeEvaluations(response(false, updatedHash));

            // Wait for the cache to expire
            Thread.sleep(500);

            // Validate the updated toggles are available
            assertThat(provider.getOctopusContext().getContentHash()).isEqualTo(updatedHash);
            assertThat(provider.getOctopusContext().evaluate("test-feature", null).getValue()).isFalse();

        } finally {
            provider.shutdown();
        }
    }

    @Test
    void whenInitialized_AndRefreshFails_RetainsExistingContextAndLogsError() throws InterruptedException {

        byte[] contentHash = {0x01, 0x02, 0x03, 0x04};

        var client = new MockOctopusFeatureClient(response(true, contentHash));

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

            // Stop warnings about refresh failures from reaching the test output
            julLogger.setUseParentHandlers(false);

            // Simulate a failed fetch
            client.changeEvaluations(null);

            // Wait for the cache to expire
            Thread.sleep(500);

            // Validate that the existing context is retained and an error was logged
            assertThat(provider.getOctopusContext().getContentHash()).isEqualTo(contentHash);
            assertThat(provider.getOctopusContext().evaluate("test-feature", null).getValue()).isTrue();
            assertThat(logMessages).anyMatch(m -> m.startsWith("Failed to retrieve updated feature flag evaluations"));

        } finally {
            julLogger.removeHandler(handler);
            julLogger.setUseParentHandlers(true);
            provider.shutdown();
        }
    }

    @Test
    void whenInitialFetchReturnsNothing_AndRefreshSucceeds_ContextIsPopulated() throws InterruptedException {

        byte[] contentHash = {0x01, 0x02, 0x03, 0x04};

        var julLogger = Logger.getLogger(OctopusClient.class.getName());
        julLogger.setUseParentHandlers(false);

        // Initialize with null so first fetch fails
        var client = new MockOctopusFeatureClient(null);
        var provider = new OctopusContextProvider(configuration, client);
        provider.initialize();

        try {
            // Check that the context is empty
            assertThat(provider.getOctopusContext().getContentHash()).isEmpty();

            // Update client to return valid toggles and wait for refresh
            client.changeEvaluations(response(false, contentHash));
            Thread.sleep(5000);

            // Assert that the context is now correctly populated
            assertThat(provider.getOctopusContext().getContentHash()).isEqualTo(contentHash);

        } finally {
            julLogger.setUseParentHandlers(true);
            provider.shutdown();
        }
    }

    @Test
    void whenRefreshReturnsNothing_AndSubsequentRefreshSucceeds_ContextIsUpdated() throws InterruptedException {

        byte[] initialHash = {0x01, 0x02, 0x03, 0x04};
        byte[] updatedHash = {0x01, 0x02, 0x03, 0x05};

        var logMessages = new CopyOnWriteArrayList<String>();
        var julLogger = Logger.getLogger(OctopusClient.class.getName());
        var handler = new Handler() {
            @Override public void publish(LogRecord record) { logMessages.add(record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        julLogger.addHandler(handler);

        // initialize with a client that returns valid toggles
        var client = new MockOctopusFeatureClient(response(true, initialHash));
        var provider = new OctopusContextProvider(configuration, client);
        provider.initialize();

        // Stop warnings about refresh failures from reaching the test output
        julLogger.setUseParentHandlers(false);

        try {
            // Switch to a null client and wait for refresh to fail
            client.changeEvaluations(null);
            Thread.sleep(5000);

            // Assert that failed refresh is logged and old context is retained
            assertThat(logMessages).anyMatch(m -> m.startsWith("Failed to retrieve updated feature flag evaluations"));
            assertThat(provider.getOctopusContext().getContentHash()).isEqualTo(initialHash);

            // Update client to return valid toggles again and wait for refresh
            client.changeEvaluations(response(false, updatedHash));
            Thread.sleep(5000);

            assertThat(provider.getOctopusContext().getContentHash()).isEqualTo(updatedHash);

        } finally {
            julLogger.removeHandler(handler);
            julLogger.setUseParentHandlers(true);
            provider.shutdown();
        }
    }

    static class ThrowsOnRefreshClient extends OctopusClient {

        static final String ERROR_MESSAGE = "Oops! Simulated refresh error";
        private final EvaluationResponse initial;

        ThrowsOnRefreshClient(EvaluationResponse initial) {
            super(null);
            this.initial = initial;
        }

        @Override
        Boolean haveFeatureFlagsChanged(byte[] contentHash) {
            throw new RuntimeException(ERROR_MESSAGE);
        }

        @Override
        EvaluationResponse getServerSideEvaluations() {
            return initial;
        }
    }

    @Test
    void whenAnExceptionIsThrownDuringRefresh_LogsErrorDetails() throws InterruptedException {

        byte[] contentHash = {0x01, 0x02, 0x03, 0x04};

        var logRecords = new CopyOnWriteArrayList<LogRecord>();
        var julLogger = Logger.getLogger(OctopusClient.class.getName());
        var handler = new Handler() {
            @Override public void publish(LogRecord record) { logRecords.add(record); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        julLogger.addHandler(handler);
        // Stop warnings about refresh failures from reaching the test output
        julLogger.setUseParentHandlers(false);

        // Initialize with a client that will throw on refresh
        var client = new ThrowsOnRefreshClient(response(true, contentHash));
        var provider = new OctopusContextProvider(configuration, client);
        provider.initialize();

        try {
            // Wait for cache to expire and refresh attempt to throw
            Thread.sleep(500);

            assertThat(logRecords).anyMatch(r ->
                r.getMessage().startsWith("Failed to retrieve updated feature flag evaluations")
                && r.getThrown() != null
                && r.getThrown().getMessage().contains(ThrowsOnRefreshClient.ERROR_MESSAGE)
            );

        } finally {
            julLogger.removeHandler(handler);
            julLogger.setUseParentHandlers(true);
            provider.shutdown();
        }
    }
}
