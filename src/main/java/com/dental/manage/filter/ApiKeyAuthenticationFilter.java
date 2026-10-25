package com.dental.manage.filter;

import com.dental.manage.context.ClinicRequestContext;
import com.dental.manage.dto.response.ApiResponse;
import com.dental.manage.entity.ApiKey;
import com.dental.manage.entity.ClinicDetails;
import com.dental.manage.enums.AgentType;
import com.dental.manage.enums.RequestSource;
import com.dental.manage.exception.InvalidCredentialsException;
import com.dental.manage.repository.ClinicDetailsRepository;
import com.dental.manage.service.ApiKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter for intercepting incoming API requests, authenticating clinic API keys,
 * and strictly enforcing that API keys are allowed only for the Retell AI voice agent.
 *
 * Enforcement Rules:
 * 1. API key authentication is strictly restricted to the Retell AI agent.
 * 2. If an API key is invalid, inactive, or expired: 401 Unauthorized.
 * 3. If an API key is not configured for Retell agent, or caller explicitly declares
 *    a non-Retell origin: 403 Forbidden.
 * 4. Requests claiming to be Retell agent without providing a valid API key: 401 Unauthorized.
 * 5. On successful authentication, establishes ClinicRequestContext with source "Retell AI agent"
 *    and request attributes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_X_API_KEY = "X-API-KEY";
    public static final String HEADER_X_CLINIC_API_KEY = "X-Clinic-API-Key";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_X_AGENT_TYPE = "X-Agent-Type";
    public static final String HEADER_X_CLIENT_TYPE = "X-Client-Type";
    public static final String HEADER_X_REQUEST_SOURCE = "X-Request-Source";
    public static final String HEADER_X_SOURCE = "X-Source";

    private final ApiKeyService apiKeyService;
    private final ClinicDetailsRepository clinicDetailsRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Permit CORS preflight requests without authentication
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = extractApiKey(request);
        String explicitAgentOrSource = extractExplicitAgentOrSource(request);

        try {
            if (apiKey != null && !apiKey.isBlank()) {
                String trimmedKey = apiKey.trim();

                ApiKey validatedKey;
                try {
                    validatedKey = apiKeyService.validateAndAuthenticateKey(trimmedKey, explicitAgentOrSource);
                } catch (InvalidCredentialsException ex) {
                    log.warn("Unauthorized request to {}: {}", request.getRequestURI(), ex.getMessage());
                    sendUnauthorizedResponse(response, ex.getMessage());
                    return;
                } catch (SecurityException ex) {
                    log.warn("Forbidden request to {}: {}", request.getRequestURI(), ex.getMessage());
                    sendForbiddenResponse(response, ex.getMessage());
                    return;
                }

                Long clinicId = validatedKey.getClinicId();
                String clinicName = clinicDetailsRepository.findByClinicIdAndIsDeletedFalse(clinicId)
                        .map(ClinicDetails::getClinicName)
                        .orElse("Smile Dental Clinic");

                log.info("Authenticated Retell AI agent via API key for clinic '{}' (clinicId={})",
                        clinicName, clinicId);

                // Populate ThreadLocal context
                ClinicRequestContext.RequestContextData contextData = ClinicRequestContext.RequestContextData.builder()
                        .clinicId(clinicId)
                        .clinicName(clinicName)
                        .apiKey(trimmedKey)
                        .source(RequestSource.RETELL_AI_AGENT)
                        .sourceName(ClinicRequestContext.RETELL_SOURCE_NAME)
                        .authenticated(true)
                        .build();
                ClinicRequestContext.setContext(contextData);

                // Populate HttpServletRequest attributes
                request.setAttribute(ClinicRequestContext.ATTRIBUTE_CLINIC_ID, clinicId);
                request.setAttribute(ClinicRequestContext.ATTRIBUTE_REQUEST_SOURCE, RequestSource.RETELL_AI_AGENT);
                request.setAttribute(ClinicRequestContext.ATTRIBUTE_SOURCE_NAME, ClinicRequestContext.RETELL_SOURCE_NAME);
                request.setAttribute(ClinicRequestContext.ATTRIBUTE_SOURCE, ClinicRequestContext.RETELL_SOURCE_NAME);
                request.setAttribute(ClinicRequestContext.ATTRIBUTE_API_KEY, trimmedKey);
                request.setAttribute("agentType", validatedKey.getAgentType());

            } else if (explicitAgentOrSource != null && !explicitAgentOrSource.isBlank()) {
                // If caller claims to be Retell agent without providing an API key, reject immediately
                if (AgentType.isRetellAgent(explicitAgentOrSource)) {
                    log.warn("Unauthorized attempt to access endpoint '{}' as Retell agent without API key",
                            request.getRequestURI());
                    sendUnauthorizedResponse(response, "Retell agent requests must authenticate using a valid API key");
                    return;
                }

                RequestSource source = RequestSource.fromString(explicitAgentOrSource);
                ClinicRequestContext.RequestContextData contextData = ClinicRequestContext.RequestContextData.builder()
                        .source(source)
                        .sourceName(source.getDisplayName())
                        .authenticated(false)
                        .build();
                ClinicRequestContext.setContext(contextData);

                request.setAttribute(ClinicRequestContext.ATTRIBUTE_REQUEST_SOURCE, source);
                request.setAttribute(ClinicRequestContext.ATTRIBUTE_SOURCE_NAME, source.getDisplayName());
                request.setAttribute(ClinicRequestContext.ATTRIBUTE_SOURCE, source.getDisplayName());
            }

            filterChain.doFilter(request, response);
        } finally {
            ClinicRequestContext.clear();
        }
    }

    private String extractApiKey(HttpServletRequest request) {
        // 1. Primary X-API-KEY header
        String apiKey = request.getHeader(HEADER_X_API_KEY);
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }

        apiKey = request.getHeader("x-api-key");
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }

        // 2. X-Clinic-API-Key header
        apiKey = request.getHeader(HEADER_X_CLINIC_API_KEY);
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }

        apiKey = request.getHeader("x-clinic-api-key");
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }

        // 3. Authorization header: Api-Key <token> or ApiKey <token> or Bearer retell_live_...
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authHeader != null && !authHeader.isBlank()) {
            String trimmed = authHeader.trim();
            if (trimmed.regionMatches(true, 0, "Api-Key ", 0, 8)) {
                return trimmed.substring(8).trim();
            }
            if (trimmed.regionMatches(true, 0, "ApiKey ", 0, 7)) {
                return trimmed.substring(7).trim();
            }
            if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String candidate = trimmed.substring(7).trim();
                if (candidate.startsWith("retell_") || candidate.startsWith("clk_") || candidate.startsWith("dent_")) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private String extractExplicitAgentOrSource(HttpServletRequest request) {
        String agent = request.getHeader(HEADER_X_AGENT_TYPE);
        if (agent != null && !agent.isBlank()) {
            return agent;
        }

        agent = request.getHeader(HEADER_X_CLIENT_TYPE);
        if (agent != null && !agent.isBlank()) {
            return agent;
        }

        String source = request.getHeader(HEADER_X_REQUEST_SOURCE);
        if (source != null && !source.isBlank()) {
            return source;
        }

        return request.getHeader(HEADER_X_SOURCE);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> errorResponse = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> errorResponse = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
