package com.octopus.openfeature.provider;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.exceptions.FlagNotFoundError;
import dev.openfeature.sdk.exceptions.ParseError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class OctopusContextTests {

    @Test
    void evaluatesToTrue_IfFeatureIsContainedWithinTheSet_AndFeatureIsEnabled() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, null).getValue()).isTrue();
    }

    @Test
    void whenEvaluatedWithCasingDifferences_EvaluationIsInsensitiveToCase() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("Test-Feature", false, null).getValue()).isTrue();
    }

    @Test
    void evaluatesToFalse_IfFeatureIsContainedWithinTheSet_AndFeatureIsNotEnabled() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", false, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, null).getValue()).isFalse();
    }

    @Test
    void givenAFlagKeyThatIsNotASlug_ThrowsFlagNotFound() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("this-is-clearly-not-a-slug", true, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThrows(FlagNotFoundError.class, () -> subject.evaluate("This is clearly not a slug!", true, null));
    }

    @Test
    void throwsFlagNotFound_IfFeatureIsNotContainedWithinSet() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("testfeature", true, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThrows(FlagNotFoundError.class, () -> subject.evaluate("anotherfeature", true, null));
    }

    @Test
    void whenAFeatureIsToggledOnForASpecificSegment_EvaluatesToTrueWhenSegmentIsSpecified() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("testfeature", true, "evaluation-key", List.of(new Segment("license", "trial")), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);

        assertThat(subject.evaluate("testfeature", false, buildContext(List.of(Map.entry("license", "trial")))).getValue()).isTrue();
        assertThat(subject.evaluate("testfeature", false, buildContext(List.of(Map.entry("other", "segment")))).getValue()).isFalse();
        assertThat(subject.evaluate("testfeature", false, null).getValue()).isFalse();
    }

    @Test
    void whenFeatureIsNotToggledOnForSpecificSegments_EvaluatesToTrueRegardlessOfSegmentSpecified() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("testfeature", true, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);

        assertThat(subject.evaluate("testfeature", false, buildContext(List.of(Map.entry("license", "trial")))).getValue()).isTrue();
        assertThat(subject.evaluate("testfeature", false, null).getValue()).isTrue();
    }

    @Test
    void whenAFeatureIsToggledOnForMultipleSegments_EvaluatesCorrectly() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation(
                        "testfeature", true, "evaluation-key",
                        Arrays.asList(new Segment("license", "trial"), new Segment("region", "au"), new Segment("region", "us")),
                        100
                )),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);

        // A matching context value is present for each toggled segment
        assertThat(subject.evaluate("testfeature", false, buildContext(Arrays.asList(Map.entry("license", "trial"), Map.entry("region", "us")))).getValue())
                .isTrue();

        // A context value is present for each toggled segment, but it is not toggled on for one of the supplied values
        assertThat(subject.evaluate("testfeature", false, buildContext(Arrays.asList(Map.entry("license", "trial"), Map.entry("region", "eu")))).getValue())
                .isFalse();

        // A matching context value is present for each toggled segment, and an additional segment is present in the provided context values
        assertThat(subject.evaluate("testfeature", false, buildContext(Arrays.asList(Map.entry("license", "trial"), Map.entry("region", "us"), Map.entry("language", "english")))).getValue())
                .isTrue();

        // A context value is present for only one of the two toggled segments
        assertThat(subject.evaluate("testfeature", false, buildContext(List.of(Map.entry("license", "trial")))).getValue())
                .isFalse();

        // No context values are present for the two toggled segments
        assertThat(subject.evaluate("testfeature", true, buildContext(List.of(Map.entry("other", "segment")))).getValue())
                .isFalse();

        // None specified
        assertThat(subject.evaluate("testfeature", true, null).getValue())
                .isFalse();
    }

    @Test
    void whenAFeatureIsToggledOnForASpecificSegment_ToleratesNullValuesInContext() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("testfeature", true, "evaluation-key", List.of(new Segment("license", "trial")), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);

        // null value for the segment key does not match
        var ctxNullLicense = new MutableContext();
        ctxNullLicense.add("license", (String) null);
        assertThat(subject.evaluate("testfeature", false, ctxNullLicense).getValue()).isFalse();

        assertThat(subject.evaluate("testfeature", false, buildContext(List.of(Map.entry("other", "segment")))).getValue()).isFalse();
        assertThat(subject.evaluate("testfeature", false, null).getValue()).isFalse();
    }

    @Test
    void whenTargetingKeyFallsWithinRolloutPercentage_AndFeatureIsNotToggledForSegments_ResolvesToTrue() {
        // "evaluation-key:targeting-key" hashes to bucket 13, which is within the rollout of 13, so the feature is enabled
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 13)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(Collections.emptyList(), "targeting-key")).getValue()).isTrue();
    }

    @Test
    void whenTargetingKeyFallsOutsideRolloutPercentage_AndFeatureIsNotToggledForSegments_ResolvesToFalse() {
        // "evaluation-key:targeting-key" hashes to bucket 13, which exceeds the rollout of 12, so the feature is disabled
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 12)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(Collections.emptyList(), "targeting-key")).getValue()).isFalse();
    }

    @Test
    void whenTargetingKeyFallsWithinRolloutPercentage_AndSegmentMatchesRequiredSegments_EvaluatesToTrue() {
        // "evaluation-key:targeting-key" hashes to bucket 13, which is within the rollout of 13, and the segment license=trial matches
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", List.of(new Segment("license", "trial")), 13)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(List.of(Map.entry("license", "trial")), "targeting-key")).getValue()).isTrue();
    }

    @Test
    void whenTargetingKeyFallsWithinRolloutPercentage_AndSegmentValueDoesNotMatchRequiredSegment_EvaluatesToFalse() {
        // "evaluation-key:targeting-key" hashes to bucket 13, which is within the rollout of 99, but the required
        // segment license=enterprise does not match the provided license=trial, so the feature is disabled
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", List.of(new Segment("license", "enterprise")), 99)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(List.of(Map.entry("license", "trial")), "targeting-key")).getValue()).isFalse();
    }

    @Test
    void whenTargetingKeyFallsOutsideRolloutPercentage_AndSegmentValueDoesNotMatchRequiredSegment_EvaluatesToFalse() {
        // "evaluation-key:targeting-key" hashes to bucket 13, which exceeds the rollout of 12, and the segment also does not match
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", List.of(new Segment("license", "enterprise")), 12)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(List.of(Map.entry("license", "trial")), "targeting-key")).getValue()).isFalse();
    }

    @Test
    void whenNoTargetingKey_RolloutIsLessThanOneHundredPercent_ResolvesToFalse() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 99)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(Collections.emptyList(), null)).getValue()).isFalse();
    }

    @Test
    void whenNoTargetingKey_RolloutIsEqualToOneHundredPercent_ResolvesToTrue() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("test-feature", true, "evaluation-key", Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        assertThat(subject.evaluate("test-feature", false, buildContext(Collections.emptyList(), null)).getValue()).isTrue();
    }

    @Test
    void shouldThrowParseErrorWhenEnabledToggleIsMissingEvaluationKey() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-a", true, null, Collections.emptyList(), 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        var ex = assertThrows(ParseError.class, () -> subject.evaluate("feature-a", false, null));
        assertThat(ex.getMessage()).contains("feature-a");
    }

    @Test
    void shouldThrowParseErrorWhenEnabledToggleIsMissingSegments() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-b", true, "evaluation-key", null, 100)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        var ex = assertThrows(ParseError.class, () -> subject.evaluate("feature-b", false, null));
        assertThat(ex.getMessage()).contains("feature-b");
    }

    @Test
    void shouldThrowParseErrorWhenEnabledToggleIsMissingClientRolloutPercentage() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-c", true, "evaluation-key", Collections.emptyList(), null)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        var ex = assertThrows(ParseError.class, () -> subject.evaluate("feature-c", false, null));
        assertThat(ex.getMessage()).contains("feature-c");
    }

    @Test
    void shouldThrowParseErrorWhenEnabledToggleIsMissingAllClientEvaluationFields() {
        var toggles = new FeatureToggles(
                List.of(new FeatureToggleEvaluation("feature-d", true, null, null, null)),
                new byte[0]
        );
        var subject = new OctopusContext(toggles);
        var ex = assertThrows(ParseError.class, () -> subject.evaluate("feature-d", true, null));
        assertThat(ex.getMessage()).contains("feature-d");
    }

    private EvaluationContext buildContext(List<Map.Entry<String, String>> entries) {
        return buildContext(entries, null);
    }

    private EvaluationContext buildContext(List<Map.Entry<String, String>> entries, String targetingKey) {
        var context = new MutableContext();
        entries.forEach(entry -> context.add(entry.getKey(), entry.getValue()));
        if (targetingKey != null) {
            context.setTargetingKey(targetingKey);
        }
        return context;
    }

    // These cases verify that getNormalizedNumber produces the same bucketing values as the equivalent
    // implementations in other Octopus OpenFeature provider libraries (e.g. .NET). The expected values
    // are derived from the reference MurmurHash3 little-endian algorithm and are duplicated verbatim
    // across all libraries. DO NOT modify the input arguments or expected values — doing so would mask
    // a real divergence in evaluation behaviour between libraries and defeat the purpose of this test.
    @ParameterizedTest(name = "[{index}] ({0}, {1}) -> {2}")
    @MethodSource("cases")
    void getNormalizedNumberMatchesExpectedValue(String evaluationKey, String targetingKey, int expected) {
        assertThat(OctopusContext.getNormalizedNumber(evaluationKey, targetingKey)).isEqualTo(expected);
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of("ef70b4c0-1773-44a3-9b95-f239ae97d9db", "780c4b16-a510-49fa-a2b2-bbd1c38dbe31", 48),
                Arguments.of("055665f0-fbb3-484e-8ef1-52125425b7b2", "6a33c211-5af8-4c34-ba82-4d79846cb045", 85),
                Arguments.of("8e63257f-07f1-43b6-871f-4e8fcaa1718e", "0a8dc2d9-fb7c-42a9-9bfd-200f65717c5f", 47),
                Arguments.of("b8a1abcd-1a69-46c7-8da4-f9fc3c6da5d7", "30433819-7096-420f-9b9f-ff555504a5dd", 79),
                Arguments.of("277582f0-93f5-4c2c-888e-44f94ecc6c7f", "3238dad5-c7d4-460e-868d-f8de4770a1e8", 97),
                Arguments.of("41f60be0-7cef-4aa3-aaed-cf4a4599a084", "user-0007", 41),
                Arguments.of("rollout", "fd7f2fc4-7ecc-46a0-9ee1-073bac59d6dd", 7),
                Arguments.of("83a52df2-49a6-47ec-89f1-b6106a2ca9a3", "a43ec79d-db51-4371-8827-cb5e7beac345", 90),
                Arguments.of("7b89296c-6dcb-4c50-8857-7eb1924770d3", "5932b247-0569-4069-9688-867975502818", 60),
                Arguments.of("7aeff54e-808c-46a6-9f49-2fba47a1fca7", "48c16871-3872-4ddc-a828-c3dd26f30f0b", 90),
                Arguments.of("b08969ee-f0d8-45e3-b763-c84ebdde03f1", "6b9bb2f6-535a-4e07-b6df-fce8112d9d11", 68),
                Arguments.of("5bc8fbbc-bde5-4099-8164-d8399f767c45", "545c10b7-eb4d-4a28-a1a6-e4dc0b46adec", 79),
                Arguments.of("5d357ffe-4423-460d-9b0e-da407f5e8e61", "2b263e0c-e7b2-4237-beb7-aa5d9f5ddbf2", 93),
                Arguments.of("8d62d777-8090-44bd-96a7-4dbe3e572e0f", "b5e8480a-d3dd-4a47-b108-781d1e644773", 60),
                Arguments.of("688bf8ca-c418-4fcd-9db8-4aa91325af10", "11eb4678-7889-4121-898d-83332035bce5", 28),
                Arguments.of("8623121d-e0bb-437a-9459-4d8b75673fca", "dff8494d-3bb3-4b25-9f81-20c447f0e17e", 76),
                Arguments.of("c33f4584-b23b-41d8-893c-d01609de8895", "48b0083a-3c39-4aba-9b9f-3e14c7608387", 85),
                Arguments.of("e008364f-306d-4b3d-bce8-0083255d4b38", "tenant-001", 84),
                Arguments.of("cc73abe9-9eba-409f-822a-87dc52e17fd9", "3df9d4e4-bda3-42f7-9b7c-85bf909d05d8", 11),
                Arguments.of("2507759b-36af-471e-ad2e-f1c113d1e9e3", "a", 13),
                Arguments.of("d7f20e07-ed42-42ed-84bb-895c608099f6", "f97f4108-4e1a-4c75-942f-8ea30236beb7", 9),
                Arguments.of("48f165d5-7b00-47f4-b81e-f86f5c8cc1ab", "8700cfed-571a-41b0-a32e-a7fbb2bec49f", 18),
                Arguments.of("46f7c9ea-b38c-445a-bad9-8a70a603e9e1", "user-0004", 32),
                Arguments.of("f7ae9466-2c20-48f7-9732-3b35156199d0", "2d885f55-3d4e-42ba-a6b5-3b4cb3362773", 82),
                Arguments.of("7450bc56-6fc6-4ccb-95b5-582a736a9625", "user-0008", 97),
                Arguments.of("b39cfd4b-8abe-4d78-8520-10116895cea8", "b86759d1-3ba6-4a4a-839f-64bfbf5c8eed", 2),
                Arguments.of("4cdac1b3-1894-45b6-b00a-65ccd081a3d4", "org-001", 74),
                Arguments.of("6b0404f2-b094-40b8-ab01-a1c12a3a2107", "d34923a2-4d4b-4d7e-a96d-7e83b47a99c9", 93),
                Arguments.of("fbc1af89-7d69-4938-84f7-18fd250c67f3", "org-003", 4),
                Arguments.of("8bb01460-217f-471c-be0a-e8fa1ceac2cc", "org-002", 37),
                Arguments.of("21636369-8b52-4b4a-97b7-50923ceb3ffd", "xxxxxxxxxxxxxx", 7),
                Arguments.of("bdd640fb-0667-4ad1-9c80-317fa3b1799d", "c927b3d7-c9df-4be2-8065-5831f2396945", 29),
                Arguments.of("705e3831-2331-4265-babf-7430e9e4817a", "87bb47cb-7f00-4264-8706-b39b55d9e4b8", 15),
                Arguments.of("bd023447-34aa-43ff-b278-e0a594ac807a", "tenant-002", 24),
                Arguments.of("cd28037c-1888-4b25-898c-cb7caf2a6a52", "user-0010", 51),
                Arguments.of("06a3f5be-62a9-401b-8279-530735b8cfae", "user-0003", 15),
                Arguments.of("eb85403c-7e8d-4475-9962-1895e98f559d", "xxxxxxxx", 71),
                Arguments.of("8dbb5b2a-6e20-4f8e-9001-a6625a1298a1", "1a8d7c50-c0d3-4ac5-88a4-3616b02b0cd8", 41),
                Arguments.of("e60922ca-8aba-43ed-a4d9-4a354580711b", "7dfb4d2e-17f5-413e-ace3-8d733ed6ff1a", 2),
                Arguments.of("6513270e-269e-4d37-b2a7-4de452e6b438", "org-005", 65),
                Arguments.of("c393fd0e-1cc6-4be5-b836-46bf0324aac3", "0ebd0956-dd97-4698-83c7-520f5783b05f", 60),
                Arguments.of("72e63ac7-a953-4322-9f70-d5dc2e675fc7", "xxxxxxxxx", 92),
                Arguments.of("07158ab7-95f3-4183-9b69-13cd87684f34", "bc087a02-3415-49ca-a6cd-1c580bad0d02", 79),
                Arguments.of("21b8c26b-c023-43ab-95da-cb8f8c773fe6", "c50c6475-83c3-4a03-b99d-b98358c913cb", 59),
                Arguments.of("e8d79f49-af6d-414c-8a6f-188a424e617b", "2f3e9786-a457-4917-b52b-e67cdbd76923", 70),
                Arguments.of("377e6ff8-8e83-4961-ae84-3b2c7e96ba87", "tenant-005", 50),
                Arguments.of("02f16d3d-8f4c-49e7-992b-0703f7467ac9", "e895eba8-39a6-4ad3-a3aa-7533875fd5a9", 86),
                Arguments.of("323d3ab0-f35c-48a9-bce5-e9d717208331", "tenant-003", 88),
                Arguments.of("d95bafc8-f2a4-427b-9cf4-bb99f4bea973", "9aa4e5cd-db41-4f65-9532-f707456606fe", 98),
                Arguments.of("993955be-5888-4f39-937c-56af8c5187c1", "76f939de-30a5-46cc-980d-e48610541d3f", 32),
                Arguments.of("d2e270f9-1fc6-4186-aebc-12938bfb2fcf", "ecd4771a-15e0-4c75-9c36-af0e659ba9df", 25),
                Arguments.of("fe1b1434-3b10-4980-950c-aef9618a9261", "6f5bb29e-1705-4cec-9f02-2d65cbbdc47d", 66),
                Arguments.of("dark-launch", "xxxxxxxxxxxx", 25),
                Arguments.of("0a3aee49-6666-4879-938d-da71e3658966", "505f886a-8692-491e-a530-3537fa5dcfb4", 13),
                Arguments.of("db5b5fab-8f4d-4e27-9da1-494c73cf256d", "xxxxxxxxxx", 44),
                Arguments.of("3bb427c1-a1da-459d-aad1-245c92010b38", "f10cc4f3-ae39-489a-81cf-e9d7627bffe1", 76),
                Arguments.of("374f469f-e3e0-4eda-9a6b-b5639dfcfbd4", "xxxxxxxxxxxxx", 58),
                Arguments.of("ee34cf80-4b49-41f1-b1b9-016371f4a4e4", "2b57b54f-05fa-4c58-a83e-395b8e9a0d48", 66),
                Arguments.of("4dad2986-ce83-4960-aa06-e9ab85a0bcc1", "9b58e9a0-ab47-4b99-a8b5-8dcf8187afc1", 78),
                Arguments.of("9af49740-96f5-40ee-9e25-02420ae59635", "user-0006", 6),
                Arguments.of("e539a78b-c8ef-4346-8b12-ae6ead581e57", "7dfa7c26-d794-4374-8ad7-57b6173beda4", 88),
                Arguments.of("e3e70682-c209-4cac-a29f-6fbed82c07cd", "ed146fe6-b963-4511-aa7b-4732d636b6ce", 38),
                Arguments.of("cd613e30-d8f1-4adf-91b7-584a2265b1f5", "193988fd-b97b-4177-bb55-68426f35f0bb", 97),
                Arguments.of("10adf348-2c4b-4d89-9344-7cbaed90dafc", "xxxxxxxxxxx", 67),
                Arguments.of("32833106-536e-45df-80b2-d002cc92d33d", "abcd", 10),
                Arguments.of("c15521b1-b3dc-450a-9daa-37e51b591d75", "a814e7ff-b207-453b-8167-7ccdeb6c1acc", 41),
                Arguments.of("de04cece-83e9-40e4-9c51-e692dc1729ca", "6347afc2-8444-4e8e-8f6d-a0d8ed54c5d8", 44),
                Arguments.of("c12776e4-6dd4-41b2-abce-fab3a3b48c4a", "user-0002", 68),
                Arguments.of("d1f36a09-9d74-4646-b388-d25833183edb", "user-0005", 86),
                Arguments.of("85750621-02fb-4d4f-b57f-bc5af71a1bfc", "abcde", 30),
                Arguments.of("feature-toggle", "tenant-004", 57),
                Arguments.of("7b24b13f-17ba-4194-b6ae-4b1d3423bf15", "ab", 19),
                Arguments.of("e149bd09-0df5-4245-84b0-6badfa7576c5", "201af639-f64b-4b83-8594-4e9cdac12538", 25),
                Arguments.of("5532e8ba-3083-449e-b945-e4b665c1d4b4", "2f3adacf-3c50-4499-8960-90bdf5d7e2fd", 39),
                Arguments.of("8e91579a-21c3-439e-90c1-91728c541241", "2ca20ef1-a0d9-47e9-8d6e-5c0baeb6ce87", 71),
                Arguments.of("c6601970-e9ff-441d-8899-3a14bb459fdf", "c3565f1d-1bda-4d13-afc1-3e734ce48c7d", 99),
                Arguments.of("c963cfe0-afae-4a3b-b909-6a04e7d80068", "user-0009", 94),
                Arguments.of("ec7d4222-6f41-4481-8fde-580f122088a5", "user-0001", 6),
                Arguments.of("988a0c48-5979-41d1-b000-368d2534c02d", "org-004", 10),
                Arguments.of("bd0558b4-9828-4a61-883e-dd4112cc16df", "xxxxxxxxxxxxxxx", 7),
                Arguments.of("a689ee27-eec1-43b6-95a8-f48f39643825", "097ac32f-0f04-4b42-9dcc-9e88dd29c0f4", 21),
                Arguments.of("9530fcd9-d6fd-4d9b-a203-2801b65c1c28", "abc", 9),
                Arguments.of("3e1c26d3-23ef-423e-a848-f808f54d35bf", "f4e663c3-d0eb-4c6c-a8c2-bf7e6628ac93", 32),
                Arguments.of("75bd5125-db54-435f-9802-db897f041728", "780c4b16-a510-49fa-a2b2-bbd1c38dbe31", 64),
                Arguments.of("8cd272e0-909e-4060-8425-07646bc9947a", "6a33c211-5af8-4c34-ba82-4d79846cb045", 23),
                Arguments.of("experiment-a", "0a8dc2d9-fb7c-42a9-9bfd-200f65717c5f", 93),
                Arguments.of("eeee3183-69ca-47e7-9826-00e9111f4efd", "30433819-7096-420f-9b9f-ff555504a5dd", 76),
                Arguments.of("b3fb08f3-4b48-438b-9e51-a921e8e6a305", "3238dad5-c7d4-460e-868d-f8de4770a1e8", 84),
                Arguments.of("14a03569-d26b-4496-92e5-dfe8cb1855fe", "user-0007", 65),
                Arguments.of("9cecdeee-a156-4927-9ff6-3c0179e58218", "fd7f2fc4-7ecc-46a0-9ee1-073bac59d6dd", 2),
                Arguments.of("568cec2b-740a-4798-96f1-813481854f8a", "a43ec79d-db51-4371-8827-cb5e7beac345", 73),
                Arguments.of("6018366c-f658-47a7-9ed3-4fe53a096533", "5932b247-0569-4069-9688-867975502818", 83),
                Arguments.of("87751d4c-a850-4e2c-84dc-da6a797d76de", "48c16871-3872-4ddc-a828-c3dd26f30f0b", 38),
                Arguments.of("4a37fa2d-f2d7-440f-8785-9faeecc3f80c", "6b9bb2f6-535a-4e07-b6df-fce8112d9d11", 68),
                Arguments.of("a8d42934-33e7-48a0-a81f-9b0cbf4e7af6", "545c10b7-eb4d-4a28-a1a6-e4dc0b46adec", 34),
                Arguments.of("9c6ab710-4a08-4720-8ede-24428a013fda", "2b263e0c-e7b2-4237-beb7-aa5d9f5ddbf2", 11),
                Arguments.of("b406dd29-9b57-4d64-8490-5c0914c25b99", "b5e8480a-d3dd-4a47-b108-781d1e644773", 28),
                Arguments.of("checkout-v2", "11eb4678-7889-4121-898d-83332035bce5", 21),
                Arguments.of("4462ebfc-5f91-4ef0-9cfb-ac6e7687a66e", "dff8494d-3bb3-4b25-9f81-20c447f0e17e", 43),
                Arguments.of("63bf9de9-f33f-4a58-b698-0fbe5edcccc1", "48b0083a-3c39-4aba-9b9f-3e14c7608387", 11),
                Arguments.of("test", "az", 1),
                Arguments.of("bucket", "j", 1),
                Arguments.of("test", "y", 100),
                Arguments.of("flag", "c", 100)
        );
    }
}
