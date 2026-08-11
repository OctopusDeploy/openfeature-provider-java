package com.octopus.openfeature.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * List handling for deserialized payloads.
 */
final class ListUtils {

    private ListUtils() {
    }

    /**
     * An unmodifiable copy of a deserialized list, or {@code null} if the property was absent.
     *
     * <p>Not {@code List.copyOf}, which rejects null elements: a null in the payload has to survive
     * deserialization so that evaluation can report it as the malformed response it is, rather than
     * failing the whole response as it is read.
     */
    static <T> List<T> copyOrNull(List<T> list) {
        return list == null ? null : Collections.unmodifiableList(new ArrayList<>(list));
    }
}
