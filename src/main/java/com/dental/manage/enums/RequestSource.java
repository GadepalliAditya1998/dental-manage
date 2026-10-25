package com.dental.manage.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Enumeration representing the origin/source of incoming API requests.
 */
public enum RequestSource {

    /**
     * Invocations originating from an automated voice/chat agent such as Retell AI.
     */
    RETELL_AI_AGENT("Retell AI agent"),

    /**
     * Invocations originating from staff actions in the clinic front-office portal.
     */
    CLINIC_PORTAL("Clinic Portal"),

    /**
     * Invocations originating from other external API integrations.
     */
    EXTERNAL_API("External API"),

    /**
     * Background tasks, scheduled jobs, or system cron executions.
     */
    SYSTEM("System"),

    /**
     * Unspecified or untracked request source.
     */
    UNKNOWN("Unknown");

    private final String displayName;

    RequestSource(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this source represents an AI voice/chat agent.
     */
    public boolean isAiAgent() {
        return this == RETELL_AI_AGENT;
    }

    /**
     * Maps this request source to the corresponding appointment BookingSource enum.
     */
    public BookingSource toBookingSource() {
        return this == RETELL_AI_AGENT ? BookingSource.AI_AGENT : BookingSource.MANUAL;
    }

    @JsonCreator
    public static RequestSource fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_");

        if (normalized.contains("RETELL") || normalized.contains("AI_AGENT") || normalized.equals("AI")) {
            return RETELL_AI_AGENT;
        }
        if (normalized.contains("PORTAL") || normalized.contains("MANUAL") || normalized.contains("CLINIC") || normalized.contains("STAFF")) {
            return CLINIC_PORTAL;
        }
        if (normalized.contains("EXTERNAL") || normalized.contains("INTEGRATION")) {
            return EXTERNAL_API;
        }
        if (normalized.contains("SYSTEM") || normalized.contains("CRON")) {
            return SYSTEM;
        }

        return UNKNOWN;
    }
}
