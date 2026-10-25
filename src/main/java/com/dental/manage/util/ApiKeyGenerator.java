package com.dental.manage.util;

import java.security.SecureRandom;

/**
 * Utility for generating cryptographically secure API keys for clinic-wide operations
 * and integrations restricted to the Retell AI agent.
 * Format: retell_live_<32 lowercase hex characters> (128 bits of cryptographic entropy).
 */
public final class ApiKeyGenerator {

    public static final String PREFIX_RETELL = "retell_live_";
    public static final String PREFIX_LEGACY = "clk_live_";
    public static final String PREFIX_DENTAL = "dent_live_";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private ApiKeyGenerator() {
        // Utility class
    }

    /**
     * Generates a new cryptographically secure API key specifically for the Retell agent.
     *
     * @return 44-character API key string starting with 'retell_live_'
     */
    public static String generateApiKey() {
        return generateApiKey(PREFIX_RETELL);
    }

    /**
     * Generates a new cryptographically secure API key with a designated prefix.
     *
     * @param prefix key prefix (e.g. 'retell_live_')
     * @return generated API key string
     */
    public static String generateApiKey(String prefix) {
        String effectivePrefix = (prefix != null && !prefix.isBlank()) ? prefix : PREFIX_RETELL;
        byte[] randomBytes = new byte[16]; // 128 bits entropy
        RANDOM.nextBytes(randomBytes);

        StringBuilder sb = new StringBuilder(effectivePrefix.length() + 32);
        sb.append(effectivePrefix);
        for (byte b : randomBytes) {
            sb.append(HEX_CHARS[(b >> 4) & 0x0F]);
            sb.append(HEX_CHARS[b & 0x0F]);
        }
        return sb.toString();
    }

    /**
     * Validates whether a candidate string conforms to a valid API key pattern.
     *
     * @param apiKey candidate key
     * @return true if non-null and matches basic length and prefix criteria
     */
    public static boolean isValidKeyFormat(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        String trimmed = apiKey.trim();
        return trimmed.length() >= 16 && (
                trimmed.startsWith(PREFIX_RETELL) ||
                trimmed.startsWith(PREFIX_LEGACY) ||
                trimmed.startsWith(PREFIX_DENTAL) ||
                trimmed.startsWith("retell_") ||
                trimmed.startsWith("clk_")
        );
    }
}
