package com.dental.manage.enums;

import java.util.Locale;

/**
 * Roles for users within the dental clinic management system.
 */
public enum UserRole {
    RECEPTIONIST,
    DOCTOR,
    PATIENT;

    /**
     * Determines whether users with this role are permitted to log in to the portal.
     * Currently, only RECEPTIONIST and DOCTOR roles are permitted.
     *
     * @return true if permitted, false otherwise
     */
    public boolean isPortalAccessAllowed() {
        return this == RECEPTIONIST || this == DOCTOR;
    }

    /**
     * Resolves UserRole from a case-insensitive string value.
     *
     * @param value role string
     * @return UserRole
     */
    public static UserRole fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        return UserRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
