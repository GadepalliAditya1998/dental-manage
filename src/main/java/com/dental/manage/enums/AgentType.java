package com.dental.manage.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Enumeration representing allowed AI voice/chat agent types that can authenticate via API keys.
 * API keys in this system are strictly restricted to the Retell AI agent.
 */
public enum AgentType {

    RETELL_AGENT("RETELL_AGENT", "Retell AI Voice Agent");

    private final String code;
    private final String displayName;

    AgentType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Determines whether a given string corresponds to the authorized Retell agent.
     *
     * @param value candidate string
     * @return true if matches Retell agent naming
     */
    public static boolean isRetellAgent(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_");
        return normalized.contains("RETELL")
                || normalized.equals("RETELL_AGENT")
                || normalized.equals("RETELL_AI_AGENT")
                || normalized.equals("RETELL_AI");
    }

    @JsonCreator
    public static AgentType fromString(String value) {
        if (isRetellAgent(value)) {
            return RETELL_AGENT;
        }
        throw new IllegalArgumentException("Unsupported agent type '" + value + "'. Only RETELL_AGENT is authorized for API keys.");
    }
}
