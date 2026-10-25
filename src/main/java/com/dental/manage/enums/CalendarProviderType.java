package com.dental.manage.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported calendar provider types (NONE, GOOGLE).
 */
public enum CalendarProviderType {
    NONE("none"),
    GOOGLE("google");

    private final String value;

    CalendarProviderType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CalendarProviderType fromString(String text) {
        if (text == null || text.isBlank()) {
            return NONE;
        }
        String normalized = text.trim().toLowerCase();
        if ("google".equals(normalized) || "gcal".equals(normalized) || "google_calendar".equals(normalized)) {
            return GOOGLE;
        }
        if ("none".equals(normalized) || "disabled".equals(normalized) || "off".equals(normalized)) {
            return NONE;
        }
        throw new IllegalArgumentException("Unknown calendar provider: " + text + ". Supported values are 'google' or 'none'.");
    }
}
