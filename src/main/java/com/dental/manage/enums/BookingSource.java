package com.dental.manage.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Source/channel from which an appointment booking originated.
 * Differentiates manual bookings made from the clinic side (staff/receptionist)
 * from automated bookings made by an AI voice/chat agent.
 */
public enum BookingSource {
    /**
     * Manual booking initiated from the clinic side (e.g. receptionist, clinic admin, portal).
     */
    MANUAL,

    /**
     * Automated booking initiated via an AI voice or chat agent (e.g. Retell AI).
     */
    AI_AGENT;

    /**
     * Parses a string representation into a BookingSource enum.
     * Supports case-insensitivity, whitespace, hyphens, and common aliases (e.g., CLINIC, AI).
     *
     * @param value raw source string
     * @return matching BookingSource enum, or null if input is null/blank
     */
    @JsonCreator
    public static BookingSource fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        switch (normalized) {
            case "MANUAL":
            case "CLINIC":
            case "CLINIC_MANUAL":
            case "STAFF":
            case "RECEPTIONIST":
            case "IN_PERSON":
                return MANUAL;
            case "AI":
            case "AI_AGENT":
            case "AGENT":
            case "RETELL":
            case "RETELL_AI":
            case "BOT":
            case "VOICE_AGENT":
                return AI_AGENT;
            default:
                try {
                    return BookingSource.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Unknown booking source: '" + value + "'. Allowed values: MANUAL, AI_AGENT (or CLINIC, AI)");
                }
        }
    }

    @JsonValue
    public String toValue() {
        return name();
    }

    public boolean isAiAgent() {
        return this == AI_AGENT;
    }
}
