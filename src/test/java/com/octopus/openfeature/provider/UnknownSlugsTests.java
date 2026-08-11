package com.octopus.openfeature.provider;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class UnknownSlugsTests {

    @Test
    void warnsOncePerSlug() {
        var unknownSlugs = new UnknownSlugs();

        assertThat(unknownSlugs.shouldWarnAbout("feature-a")).isTrue();
        assertThat(unknownSlugs.shouldWarnAbout("feature-a")).isFalse();
        assertThat(unknownSlugs.shouldWarnAbout("feature-b")).as("a different slug still warns").isTrue();
    }

    @Test
    void treatsCasingDifferencesAsTheSameSlug() {
        var unknownSlugs = new UnknownSlugs();

        assertThat(unknownSlugs.shouldWarnAbout("Feature-A")).isTrue();
        assertThat(unknownSlugs.shouldWarnAbout("feature-a")).isFalse();
    }

    @Test
    void treatsCasingDifferencesAsTheSameSlugWhateverTheDefaultLocale() {
        // Under a Turkish default locale, "MY-FLAG-I".toLowerCase() is a dotless "my-flag-ı", which would
        // not match "my-flag-i" and so would warn twice for one slug.
        var previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            var unknownSlugs = new UnknownSlugs();

            assertThat(unknownSlugs.shouldWarnAbout("MY-FLAG-I")).isTrue();
            assertThat(unknownSlugs.shouldWarnAbout("my-flag-i")).isFalse();
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void toleratesANullSlug() {
        var unknownSlugs = new UnknownSlugs();

        assertThat(unknownSlugs.shouldWarnAbout(null)).isTrue();
        assertThat(unknownSlugs.shouldWarnAbout(null)).isFalse();
    }

    @Test
    void stopsGrowingOnceFull() {
        var unknownSlugs = new UnknownSlugs();
        for (int i = 0; i < 1_000; i++) {
            unknownSlugs.shouldWarnAbout("feature-" + i);
        }

        // Full: a slug already seen still does not warn, and a new one warns every time rather than being
        // remembered, so the set cannot grow without bound.
        assertThat(unknownSlugs.shouldWarnAbout("feature-0")).isFalse();
        assertThat(unknownSlugs.shouldWarnAbout("feature-new")).isTrue();
        assertThat(unknownSlugs.shouldWarnAbout("feature-new")).isTrue();
    }
}
