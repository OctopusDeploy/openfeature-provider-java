package com.octopus.openfeature.provider;

import org.apache.commons.codec.digest.MurmurHash3;

import java.nio.charset.StandardCharsets;

/**
 * Buckets a targeting key into a percentage rollout. Hashing the flag's evaluation key together with
 * the targeting key keeps a bucket stable across evaluations, while giving each flag an independent
 * spread of targeting keys.
 *
 * <p>Shared by v3 and v4 so a rollout lands on the same users whichever flag version resolved it, and
 * matching the equivalent implementations in the other Octopus OpenFeature provider libraries.
 *
 * <p>This type is public only because Java package access is not hierarchical: the v4 evaluation
 * lives in a sub-package and cannot see package-private members here. It is internal to the provider
 * and not part of the supported API. Once v3 no longer needs it, it belongs in the v4 package as a
 * package-private type, as the .NET provider has it.
 */
public final class PercentageRollout {

    private PercentageRollout() {
    }

    /**
     * Whether {@code targetingKey} falls within the first {@code percentage} percent of targeting keys
     * for the flag identified by {@code evaluationKey}.
     */
    public static boolean includes(String evaluationKey, String targetingKey, int percentage) {
        return getNormalizedNumber(evaluationKey, targetingKey) <= percentage;
    }

    /**
     * A deterministic bucket in the inclusive range 1–100 for the given evaluation and targeting keys.
     *
     * <p>Exposed rather than private so the shared cross-library test vectors can assert on the bucket
     * itself, and because v3 compares against it directly.
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
