package com.octopus.openfeature.provider;

class OctopusContextProvider {
    private final OctopusConfiguration config;
    private final OctopusClient client;
    private boolean initialized = false;
    private OctopusContext currentContext = OctopusContext.empty();
    private Thread refreshThread;
    private static final System.Logger logger = System.getLogger(OctopusClient.class.getName());

    OctopusContextProvider(OctopusConfiguration config, OctopusClient client) {
        this.config = config;
        this.client = client;
    }
    
    OctopusContext getOctopusContext() { return currentContext; }

    void initialize() {
        if (initialized) {
            return;
        }

        try {
            var toggles = client.getFeatureToggleEvaluationManifest();
            currentContext = toggles == null ? OctopusContext.empty() : new OctopusContext(toggles);
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to retrieve feature manifest during initialization. Falling back to empty context, defaults will be used during evaluation.", e);
            currentContext = OctopusContext.empty();
        }

        // run the refresh loop in the background
        refreshThread = new Thread(this::refresh);
        refreshThread.start();
        
        initialized = true;
    }

    /* 
     This method will retry forever on failures, until a shutdown event triggers the cancellation token.
     We never want to cease trying to refresh the evaluation context while the provider is still alive,
     otherwise the state will be left stale whilst the consumer continues to make use it.
     */
    void refresh() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(config.getCacheDuration().toMillis());

                if (client.haveFeatureTogglesChanged(currentContext.getContentHash())) {
                    var toggles = client.getFeatureToggleEvaluationManifest();
                    if (toggles != null) {
                        currentContext = new OctopusContext(toggles);
                    } else {
                        logger.log(System.Logger.Level.ERROR, "Failed to retrieve updated feature manifest. Retaining existing context which may be stale.");
                    }
                }
            } catch (InterruptedException e) {
                // the loop will be terminated and the thread will finish
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Failed to retrieve updated feature manifest. Retaining existing context which may be stale.", e);
            }
        }
    }
    
    void shutdown() {
        if (refreshThread != null) {
            refreshThread.interrupt();
        }
    }
}