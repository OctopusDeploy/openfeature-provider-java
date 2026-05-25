package com.octopus.openfeature.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductMetadataTests {

    @Test
    void constructor_withValidNameChars_setsNameUnchanged() {
        var metadata = new ProductMetadata("OctopusDeploy");

        assertThat(metadata.getName()).isEqualTo("OctopusDeploy");
    }

    @Test
    void constructor_withCommonUnsupportedCharsInName_stripsThemOut() {
        // Characters that may be used but are not RFC 9110 tchars
        var metadata = new ProductMetadata("My ,Product (v2.0)/release@2024:final");

        assertThat(metadata.getName()).isEqualTo("MyProductv2.0release2024final");
    }

    @Test
    void constructor_withHyphenInName_preservesIt() {
        var metadata = new ProductMetadata("My-Product");

        assertThat(metadata.getName()).isEqualTo("My-Product");
    }

    @Test
    void constructor_whenNoVersionProvided_setsEmpty() {
        var metadata = new ProductMetadata("MyProduct");

        assertThat(metadata.getVersion()).isEmpty();
    }

    @Test
    void constructor_withValidCharsInVersion_setsVersionUnchanged() {
        var metadata = new ProductMetadata("MyProduct", "2024.1.0");

        assertThat(metadata.getVersion()).get().isEqualTo("2024.1.0");
    }

    @Test
    void constructor_withUnsupportedCharsInVersion_stripsThemOut() {
        var metadata = new ProductMetadata("MyProduct", "2024.1 (beta)");

        assertThat(metadata.getVersion()).get().isEqualTo("2024.1beta");
    }

    @Test
    void constructor_whenNameBecomesEmptyAfterCleaning_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new ProductMetadata("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name");
    }

    @Test
    void constructor_whenVersionBecomesEmptyAfterCleaning_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new ProductMetadata("MyProduct", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product version");
    }
}
