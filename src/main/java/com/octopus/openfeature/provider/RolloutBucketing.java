package com.octopus.openfeature.provider;

import org.apache.commons.codec.digest.MurmurHash3;

import java.nio.charset.StandardCharsets;

/**
 * Buckets a targeting key into 1–100 for percentage rollouts. Shared by v3 and v4 so a rollout lands
 * on the same users whichever flag version resolved it, and matching the equivalent implementations
 * in the other Octopus OpenFeature provider libraries.
 *
 * <p>This type is public only because Java package access is not hierarchical: the v4 evaluation
 * lives in a sub-package and cannot see package-private members here. It is internal to the provider
 * and not part of the supported API.
 */
public final class RolloutBucketing {

    private RolloutBucketing() {
    }

    /**
     * The bucket, 1–100, that {@code targetingKey} falls into for {@code evaluationKey}. A rollout of
     * N% includes every bucket up to and including N.
     */
    public static int getNormalizedNumber(String evaluationKey, String targetingKey) {
        byte[] bytes = (evaluationKey + ":" + targetingKey).getBytes(StandardCharsets.UTF_8);

        // MurmurHash3 32-bit, seed 0. hash32x86 processes tail bytes in little-endian order,
        // matching the reference C spec and equivalent to .NET's MurmurHash.Create32() +
        // BinaryPrimitives.ReadUInt32LittleEndian().
        int hash = MurmurHash3.hash32x86(bytes, 0, bytes.length, 0);

        // Java has no unsigned integer type. Integer.toUnsignedLong() reinterprets the signed
        // int as an unsigned 32-bit value (widened to long) — equivalent to casting to uint in C#.
        long unsignedHash = Integer.toUnsignedLong(hash);

        return (int) (unsignedHash % 100) + 1;
    }
}
