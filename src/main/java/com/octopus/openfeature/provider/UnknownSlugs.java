package com.octopus.openfeature.provider;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers the slugs already warned about, so an unrecognised slug is reported once rather than on
 * every evaluation of it.
 *
 * <p>Owned by the cache rather than by an evaluator, because an evaluator is replaced whenever the
 * response changes — putting this on one would forget every slug about every cache interval, which is
 * the opposite of the intent. Bounded because slugs come from the caller, who may derive them (say,
 * one per tenant) and would otherwise grow this without limit for the life of the process.
 */
class UnknownSlugs {

    /**
     * Enough that a real application's flags all fit, small enough to stay negligible. Once full,
     * warnings simply stop being deduplicated rather than the set growing.
     */
    private static final int LIMIT = 1_000;

    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    /**
     * Whether this slug should be warned about now, recording it so the next call returns false.
     */
    boolean shouldWarnAbout(String slug) {
        // Locale.ROOT, not the default locale: under a Turkish locale "MY-FLAG-I" lowercases to a dotless
        // "my-flag-ı", which would not match "my-flag-i" and so would warn twice for one slug.
        var key = slug == null ? "" : slug.toLowerCase(Locale.ROOT);

        if (warned.size() >= LIMIT) {
            return !warned.contains(key);
        }

        return warned.add(key);
    }
}
