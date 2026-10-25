package com.dental.manage.context;

import com.dental.manage.enums.BookingSource;
import com.dental.manage.enums.RequestSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thread-local request context storing metadata about the current incoming API request,
 * such as the authenticated clinic ID and origin source (e.g. Retell AI agent).
 * Always cleared at the end of the request lifecycle to avoid thread pool pollution.
 */
public final class ClinicRequestContext {

    public static final String ATTRIBUTE_CLINIC_ID = "clinicId";
    public static final String ATTRIBUTE_SOURCE = "source";
    public static final String ATTRIBUTE_SOURCE_NAME = "sourceName";
    public static final String ATTRIBUTE_REQUEST_SOURCE = "requestSource";
    public static final String ATTRIBUTE_API_KEY = "apiKey";

    public static final String RETELL_SOURCE_NAME = "Retell AI agent";

    private static final ThreadLocal<RequestContextData> CURRENT_CONTEXT = new ThreadLocal<>();

    private ClinicRequestContext() {
        // Utility class
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestContextData {
        private Long clinicId;
        private String clinicName;
        private String apiKey;
        private RequestSource source;
        private String sourceName;
        private boolean authenticated;
    }

    public static void setContext(RequestContextData context) {
        CURRENT_CONTEXT.set(context);
    }

    public static RequestContextData getContext() {
        return CURRENT_CONTEXT.get();
    }

    public static Long getClinicId() {
        RequestContextData data = CURRENT_CONTEXT.get();
        return data != null ? data.getClinicId() : null;
    }

    public static String getClinicName() {
        RequestContextData data = CURRENT_CONTEXT.get();
        return data != null ? data.getClinicName() : null;
    }

    public static String getApiKey() {
        RequestContextData data = CURRENT_CONTEXT.get();
        return data != null ? data.getApiKey() : null;
    }

    public static RequestSource getSource() {
        RequestContextData data = CURRENT_CONTEXT.get();
        return data != null && data.getSource() != null ? data.getSource() : RequestSource.UNKNOWN;
    }

    public static String getSourceName() {
        RequestContextData data = CURRENT_CONTEXT.get();
        return data != null ? data.getSourceName() : null;
    }

    public static boolean isAuthenticated() {
        RequestContextData data = CURRENT_CONTEXT.get();
        return data != null && data.isAuthenticated();
    }

    /**
     * Determines whether the current request originates from the Retell AI agent.
     */
    public static boolean isRetellAiAgent() {
        RequestContextData data = CURRENT_CONTEXT.get();
        if (data == null) {
            return false;
        }
        return data.getSource() == RequestSource.RETELL_AI_AGENT
                || RETELL_SOURCE_NAME.equalsIgnoreCase(data.getSourceName());
    }

    /**
     * Resolves the appropriate appointment booking source based on the active request context.
     */
    public static BookingSource getBookingSource() {
        if (isRetellAiAgent()) {
            return BookingSource.AI_AGENT;
        }
        RequestContextData data = CURRENT_CONTEXT.get();
        return data != null && data.getSource() != null
                ? data.getSource().toBookingSource()
                : BookingSource.MANUAL;
    }

    /**
     * Clears the ThreadLocal context. Must be invoked in a finally block after each request.
     */
    public static void clear() {
        CURRENT_CONTEXT.remove();
    }

    /**
     * Helper to build and set a Retell AI agent context for the given clinic and API key.
     */
    public static RequestContextData setRetellAiContext(Long clinicId, String clinicName, String apiKey) {
        RequestContextData data = RequestContextData.builder()
                .clinicId(clinicId)
                .clinicName(clinicName)
                .apiKey(apiKey)
                .source(RequestSource.RETELL_AI_AGENT)
                .sourceName(RETELL_SOURCE_NAME)
                .authenticated(true)
                .build();
        setContext(data);
        return data;
    }
}
