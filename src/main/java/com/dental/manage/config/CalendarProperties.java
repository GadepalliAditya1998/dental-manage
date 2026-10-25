package com.dental.manage.config;

import com.dental.manage.enums.CalendarProviderType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for calendar integration (Google Calendar).
 */
@Data
@Component
@ConfigurationProperties(prefix = "calendar")
public class CalendarProperties {

    /**
     * Active calendar provider (NONE or GOOGLE).
     */
    private CalendarProviderType provider = CalendarProviderType.GOOGLE;

    /**
     * Google Calendar specific configurations.
     */
    private GoogleProperties google = new GoogleProperties();

    @Data
    public static class GoogleProperties {
        /**
         * Whether Google Calendar sync is enabled.
         */
        private boolean enabled = true;

        /**
         * Google Calendar ID (default is "primary").
         */
        private String calendarId = "primary";

        /**
         * Application name sent to Google Calendar API client.
         */
        private String applicationName = "DentalClinicManagement";

        /**
         * File path to Service Account JSON credentials file.
         */
        private String credentialsPath;

        /**
         * Raw Service Account JSON content string (useful for environment variables).
         */
        private String credentialsJson;

        /**
         * Google OAuth2 Client ID (if using OAuth2 user credentials).
         */
        private String clientId;

        /**
         * Google OAuth2 Client Secret (if using OAuth2 user credentials).
         */
        private String clientSecret;

        /**
         * Google OAuth2 Refresh Token (if using OAuth2 user credentials).
         */
        private String refreshToken;

        /**
         * How to send notifications to attendees ("all", "external_only", "none").
         * "all" triggers Google Calendar invites directly to both doctor's and patient's calendars.
         */
        private String sendUpdates = "all";

        /**
         * Default timezone to use if clinic timezone is not configured.
         */
        private String defaultTimezone = "Asia/Kolkata";
    }
}
