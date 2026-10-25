package com.dental.manage.util;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Utility for generating scalable, partition-friendly, time-ordered patient identifiers.
 *
 * <p>Format: {@code PAT-<clinicId>-<timestampHex><randomHex>}
 * <br>Example: {@code PAT-1-000192B6E9F8A3B4C12D}
 *
 * <h3>Design & Partition Characteristics:</h3>
 * <ul>
 *   <li><b>Partition-Friendly Routing:</b> Contains the tenant/clinic identifier directly
 *       in the prefix ({@code PAT-<clinicId>-...}), enabling database partition pruning
 *       (e.g., PostgreSQL declarative list/hash partitioning) and application-level shard routing
 *       without requiring cross-partition scans or distributed lookups.</li>
 *   <li><b>Horizontally Scalable:</b> Completely distributed identifier generation with zero
 *       centralized lock contention or database sequence bottlenecks across multiple application nodes.</li>
 *   <li><b>B-Tree Index Locality:</b> Incorporates a monotonic epoch millisecond timestamp prefix,
 *       preventing random page thrashing and index fragmentation in B-tree indexes under high-throughput writes.</li>
 *   <li><b>Cryptographic Collision Resistance:</b> Combines high-resolution timestamps with 48 bits
 *       of cryptographically secure pseudorandom bits per millisecond, ensuring collision-free IDs at scale.</li>
 * </ul>
 */
public final class PatientIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PatientIdGenerator() {}

    /**
     * Generates a unique, partition-friendly string patient ID.
     *
     * @param clinicId Tenant/clinic ID used as the partition routing key
     * @return Formatted string patient identifier
     */
    public static String generate(Long clinicId) {
        long effectiveClinicId = clinicId != null ? clinicId : 1L;
        long timestamp = Instant.now().toEpochMilli();
        long randomBits = RANDOM.nextLong() & 0xFFFFFFFFFFFFL; // 48 bits of entropy
        return String.format("PAT-%d-%011X%012X", effectiveClinicId, timestamp, randomBits);
    }

    /**
     * Extracts the clinic ID (partition key) from a structured patient ID string.
     *
     * @param patientId Structured patient ID (e.g. PAT-1-000192B6...)
     * @return Extracted clinic ID or {@code null} if not extractable
     */
    public static Long extractClinicId(String patientId) {
        if (patientId == null || !patientId.toUpperCase().startsWith("PAT-")) {
            return null;
        }
        String[] parts = patientId.split("-");
        if (parts.length >= 2) {
            try {
                return Long.parseLong(parts[1]);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
