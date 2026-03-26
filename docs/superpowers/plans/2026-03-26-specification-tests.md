# Specification Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a parameterised integration test harness that reads shared JSON fixture files from a git submodule and runs each test case against a fake HTTP server.

**Architecture:** A `Server` test helper wraps WireMock and maps unique Bearer tokens to fixture response bodies via per-token stubs. `SpecificationTests` uses JUnit 5 `@ParameterizedTest` + `@MethodSource` to load fixture files from `specification/Fixtures/*.json`, yielding one named test per case. Each test configures the fake server, points `OctopusProvider` at it, evaluates the flag, and asserts value and error code.

**Tech Stack:** JUnit 5 (already present), WireMock 3.x (`org.wiremock:wiremock`, new test dependency), Jackson (already present), OpenFeature Java SDK (already present)

---

## File Map

| Action | File | Purpose |
|---|---|---|
| Modify | `pom.xml` | Add WireMock test dependency |
| Shell | `git submodule add ...` | Clone spec repo to `specification/` |
| Modify | `src/main/java/com/octopus/openfeature/provider/OctopusConfiguration.java` | Add `serverUri` field + package-private setter |
| Create | `src/test/java/com/octopus/openfeature/provider/OctopusConfigurationTests.java` | Test for configurable server URI |
| Create | `src/test/java/com/octopus/openfeature/provider/Server.java` | WireMock wrapper for fake toggle API |
| Create | `src/test/java/com/octopus/openfeature/provider/SpecificationTests.java` | Parameterised specification tests + fixture models |
| Modify | `.github/workflows/ci.yml` | Add `submodules: true` to checkout step |

---

## Task 1: Add WireMock dependency

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add WireMock to pom.xml**

In `pom.xml`, add the following inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock</artifactId>
    <version>3.5.4</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Verify existing tests still pass**

Run: `mvn test -B`

Expected output includes:
```
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add WireMock test dependency"
```

---

## Task 2: Add git submodule

**Files:**
- Shell: `git submodule add`

- [ ] **Step 1: Add the specification submodule**

Run from the project root:
```bash
git submodule add https://github.com/OctopusDeploy/openfeature-provider-specification.git specification
```

- [ ] **Step 2: Verify the fixture file is present**

Run: `ls specification/Fixtures/`

Expected output:
```
simple-value-only-toggles.json
```

- [ ] **Step 3: Commit**

```bash
git add .gitmodules specification
git commit -m "chore: add openfeature-provider-specification as git submodule"
```

---

## Task 3: Make OctopusConfiguration server URI configurable

**Files:**
- Create: `src/test/java/com/octopus/openfeature/provider/OctopusConfigurationTests.java`
- Modify: `src/main/java/com/octopus/openfeature/provider/OctopusConfiguration.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/octopus/openfeature/provider/OctopusConfigurationTests.java`:

```java
package com.octopus.openfeature.provider;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class OctopusConfigurationTests {

    @Test
    void defaultServerUriIsOctopusCloud() {
        var config = new OctopusConfiguration("test-client");
        assertThat(config.getServerUri()).isEqualTo(URI.create("https://features.octopus.com"));
    }

    @Test
    void serverUriCanBeOverridden() {
        var config = new OctopusConfiguration("test-client");
        var customUri = URI.create("http://localhost:8080");
        config.setServerUri(customUri);
        assertThat(config.getServerUri()).isEqualTo(customUri);
    }
}
```

- [ ] **Step 2: Run to confirm failure**

Run: `mvn test -Dtest=OctopusConfigurationTests -B`

Expected: `COMPILATION ERROR` or `BUILD FAILURE` — `setServerUri` does not yet exist.

- [ ] **Step 3: Implement setServerUri in OctopusConfiguration**

Replace the content of `src/main/java/com/octopus/openfeature/provider/OctopusConfiguration.java` with:

```java
package com.octopus.openfeature.provider;

import java.net.URI;
import java.time.Duration;

public class OctopusConfiguration {
    private final String clientIdentifier;
    private static final String DEFAULT_SERVER_URI = "https://features.octopus.com";
    private URI serverUri = URI.create(DEFAULT_SERVER_URI);
    private Duration cacheDuration = Duration.ofMinutes(1);

    public OctopusConfiguration(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
    }

    public String getClientIdentifier() { return clientIdentifier; }

    public URI getServerUri() { return serverUri; }

    // Package-private: visible to tests in same package, not to library consumers.
    void setServerUri(URI serverUri) { this.serverUri = serverUri; }

    public Duration getCacheDuration() {
        return cacheDuration;
    }

    public Duration setCacheDuration(Duration cacheDuration) {
        this.cacheDuration = cacheDuration;
        return this.cacheDuration;
    }
}
```

- [ ] **Step 4: Run the tests to confirm they pass**

Run: `mvn test -B`

Expected output includes:
```
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/octopus/openfeature/provider/OctopusConfiguration.java \
        src/test/java/com/octopus/openfeature/provider/OctopusConfigurationTests.java
git commit -m "feat: make OctopusConfiguration server URI configurable (package-private)"
```

---

## Task 4: Create the Server test helper

**Files:**
- Create: `src/test/java/com/octopus/openfeature/provider/Server.java`

- [ ] **Step 1: Create Server.java**

Create `src/test/java/com/octopus/openfeature/provider/Server.java`:

```java
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
        wireMock.stubFor(get(urlPathEqualTo("/api/featuretoggles/v3/"))
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
```

- [ ] **Step 2: Verify it compiles**

Run: `mvn test-compile -B`

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/octopus/openfeature/provider/Server.java
git commit -m "test: add WireMock Server helper for specification tests"
```

---

## Task 5: Create SpecificationTests

**Files:**
- Create: `src/test/java/com/octopus/openfeature/provider/SpecificationTests.java`

- [ ] **Step 1: Create SpecificationTests.java**

Create `src/test/java/com/octopus/openfeature/provider/SpecificationTests.java`:

```java
package com.octopus.openfeature.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FlagEvaluationDetails;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.OpenFeatureAPI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificationTests {

    private static Server server;

    @BeforeAll
    static void startServer() {
        server = new Server();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @AfterEach
    void shutdownApi() throws Exception {
        OpenFeatureAPI.getInstance().shutdown();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtureTestCases")
    void evaluate(String description, String responseJson, FixtureCase testCase) throws Exception {
        String token = server.configure(responseJson);
        OctopusConfiguration config = new OctopusConfiguration(token);
        config.setServerUri(URI.create(server.baseUrl()));

        OpenFeatureAPI api = OpenFeatureAPI.getInstance();
        api.setProviderAndWait(new OctopusProvider(config));
        Client client = api.getClient();

        EvaluationContext ctx = buildContext(testCase.configuration.context);
        FlagEvaluationDetails<Boolean> result = client.getBooleanDetails(
            testCase.configuration.slug,
            testCase.configuration.defaultValue,
            ctx
        );

        assertThat(result.getValue())
            .as(description + " → value")
            .isEqualTo(testCase.expected.value);
        assertThat(result.getErrorCode())
            .as(description + " → errorCode")
            .isEqualTo(mapErrorCode(testCase.expected.errorCode));
    }

    static Stream<Arguments> fixtureTestCases() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Path> jsonFiles;
        try (Stream<Path> files = Files.list(Path.of("specification", "Fixtures"))) {
            jsonFiles = files
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .collect(Collectors.toList());
        }
        return jsonFiles.stream().flatMap(path -> {
            try {
                Fixture fixture = mapper.readValue(path.toFile(), Fixture.class);
                String responseJson = fixture.response.toString();
                return Stream.of(fixture.cases)
                    .map(c -> Arguments.of(c.description, responseJson, c));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static EvaluationContext buildContext(Map<String, String> context) {
        MutableContext ctx = new MutableContext();
        if (context != null) {
            context.forEach(ctx::add);
        }
        return ctx;
    }

    private static ErrorCode mapErrorCode(String code) {
        if (code == null) return null;
        switch (code) {
            case "FLAG_NOT_FOUND":       return ErrorCode.FLAG_NOT_FOUND;
            case "PARSE_ERROR":          return ErrorCode.PARSE_ERROR;
            case "TYPE_MISMATCH":        return ErrorCode.TYPE_MISMATCH;
            case "TARGETING_KEY_MISSING": return ErrorCode.TARGETING_KEY_MISSING;
            case "PROVIDER_NOT_READY":   return ErrorCode.PROVIDER_NOT_READY;
            case "INVALID_CONTEXT":      return ErrorCode.INVALID_CONTEXT;
            case "PROVIDER_FATAL":       return ErrorCode.PROVIDER_FATAL;
            case "GENERAL":              return ErrorCode.GENERAL;
            default: throw new IllegalArgumentException("Unknown error code in fixture: " + code);
        }
    }

    // ---- Fixture model classes ----

    static class Fixture {
        public JsonNode response;
        public FixtureCase[] cases;
    }

    static class FixtureCase {
        public String description;
        public FixtureConfiguration configuration;
        public FixtureExpected expected;
    }

    static class FixtureConfiguration {
        public String slug;
        public boolean defaultValue;
        public Map<String, String> context;
    }

    static class FixtureExpected {
        public boolean value;
        public String errorCode;
    }
}
```

- [ ] **Step 2: Run the specification tests**

Run: `mvn test -B`

Expected output includes:
```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... -- in com.octopus.openfeature.provider.SpecificationTests
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The 4 new tests correspond to the 4 cases in `specification/Fixtures/simple-value-only-toggles.json`.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/octopus/openfeature/provider/SpecificationTests.java
git commit -m "test: add specification tests harness (DEVEX-138)"
```

---

## Task 6: Update CI workflow

**Files:**
- Modify: `.github/workflows/ci.yml`

- [ ] **Step 1: Add submodules: true to the checkout step**

Replace the content of `.github/workflows/ci.yml` with:

```yaml
name: Build and Test

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:

    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4
      with:
        submodules: true
    - name: Set up JDK 11
      uses: actions/setup-java@v4
      with:
        java-version: '11'
        distribution: 'temurin'
        cache: maven
    - name: Build with Maven
      run: mvn -B package --file pom.xml
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: fetch git submodules on checkout"
```
