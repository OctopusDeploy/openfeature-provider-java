# Specification Tests — Design

**Date:** 2026-03-26
**Linear:** DEVEX-138
**ADR:** [Sharing client library tests](https://whimsical.com/octopusdeploy/adr-sharing-client-library-tests-8QPKL2XSw9SFec4zJENBo9)
**C# reference:** [openfeature-provider-dotnet#44](https://github.com/OctopusDeploy/openfeature-provider-dotnet/pull/44)

## Context

The OpenFeature provider specification repository (`OctopusDeploy/openfeature-provider-specification`) holds shared JSON fixture files used to drive cross-language integration tests. The Java client needs a test harness that reads these fixtures and runs each case as a separate named test against a fake HTTP server, mirroring the approach taken in the C# client.

## Scope

Implement the Java test harness only. Adding new fixture files to the specification repository is out of scope for this work.

## Design

### 1. Production code change — configurable server URI

`OctopusConfiguration.getServerUri()` currently returns a hardcoded `https://features.octopus.com`. A private `serverUri` field will be added, defaulting to that value. A package-private setter `void setServerUri(URI uri)` will allow tests in the same package to override it without exposing the setter to library consumers (Java equivalent of C# `internal`).

### 2. New test classes

Both in `src/test/java/com/octopus/openfeature/provider/`.

#### `Server`

Wraps a `WireMockServer` started and stopped once per test class. Exposes:

```
String configure(String responseJson)
```

This stores the JSON keyed by a randomly generated UUID and returns that UUID as the client identifier token. A custom WireMock `ResponseTransformer` intercepts every request to `/api/featuretoggles/v3/`, reads the `Authorization: Bearer <token>` header, looks up the stored JSON, and returns it with a `ContentHash` response header (stable base64-encoded value). Requests with an unrecognised token receive a 401.

The background refresh thread in `OctopusContextProvider` polls the check endpoint (`/api/featuretoggles/check/v3/`) only after the configured `cacheDuration` elapses (default: 1 minute). Since each test completes and shuts down the provider in milliseconds, the check endpoint will not be called in practice.

#### `SpecificationTests`

```java
@ParameterizedTest(name = "{0}")
@MethodSource("fixtureTestCases")
void evaluate(String description, String responseJson, FixtureCase testCase) { ... }
```

The `fixtureTestCases()` static method:
1. Walks `specification/Fixtures/*.json`
2. Deserialises each file with Jackson (existing dependency) into a `Fixture` record containing a raw `response` JSON string and a `FixtureCase[]`
3. Yields one `Arguments.of(description, responseJson, testCase)` per case

The `description` field becomes the parameterised test display name (argument `{0}`).

Each test invocation:
1. Calls `server.configure(responseJson)` to get a unique client identifier
2. Creates `OctopusConfiguration` with that identifier, then calls `setServerUri` to point at the WireMock base URL
3. Calls `OpenFeatureAPI.getInstance().setProvider(provider)` and sets the evaluation context from the fixture
4. Evaluates the flag via `client.getBooleanDetails(slug, defaultValue)`
5. Asserts `result.getValue()` and `result.getErrorCode()` match the fixture expectations
6. Calls `OpenFeatureAPI.getInstance().shutdown()` to stop the background refresh thread

Error codes are mapped from fixture strings (e.g. `"FLAG_NOT_FOUND"`) to `ErrorCode` enum values. An unrecognised string throws `IllegalArgumentException` — a fixture bug should fail loudly.

### 3. Infrastructure changes

| Change | Detail |
|---|---|
| Git submodule | `specification/` → `https://github.com/OctopusDeploy/openfeature-provider-specification` |
| `pom.xml` | Add `org.wiremock:wiremock` (test scope) |
| CI workflow | Add `submodules: true` to the `actions/checkout` step |

## Out of scope

- Adding new fixture files to the specification repository (separate ticket)
- Removing existing `OctopusContextTests` cases now covered by fixtures (can be done once the fixture set is expanded)
