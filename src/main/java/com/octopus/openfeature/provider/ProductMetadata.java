package com.octopus.openfeature.provider;

import java.util.Optional;
import java.util.regex.Pattern;

public class ProductMetadata {

    // https://www.rfc-editor.org/rfc/rfc9110.html#name-tokens
    private static final Pattern UNSUPPORTED_CHARS = Pattern.compile("[^a-zA-Z0-9!#$%&'*+\\-.^_`|~]");

    private final String name;
    private final String version;

    public ProductMetadata(String name) {
        this.name = clean(name);
        this.version = null;

        validateName();
    }

    public ProductMetadata(String name, String version) {
        this.name = clean(name);
        this.version = clean(version);

        validateName();
        validateVersion();
    }

    public String getName() {
        return name;
    }

    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return UNSUPPORTED_CHARS.matcher(value).replaceAll("");
    }

    private void validateName() {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must contain at least one valid token character.");
        }
    }

    private void validateVersion() {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Product version must contain at least one valid token character.");
        }
    }
}
