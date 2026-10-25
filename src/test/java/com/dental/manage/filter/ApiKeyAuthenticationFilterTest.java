package com.dental.manage.filter;

import com.dental.manage.context.ClinicRequestContext;
import com.dental.manage.entity.ApiKey;
import com.dental.manage.entity.ClinicDetails;
import com.dental.manage.enums.BookingSource;
import com.dental.manage.enums.RequestSource;
import com.dental.manage.exception.InvalidCredentialsException;
import com.dental.manage.repository.ClinicDetailsRepository;
import com.dental.manage.service.ApiKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private ClinicDetailsRepository clinicDetailsRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private ApiKeyAuthenticationFilter filter;

    private ApiKey validRetellKey;
    private ClinicDetails clinic;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthenticationFilter(apiKeyService, clinicDetailsRepository, objectMapper);

        validRetellKey = ApiKey.builder()
                .clinicId(1L)
                .keyName("Retell AI Voice Agent")
                .apiKey("retell_live_clinic_1_sk_sample_key_2026")
                .agentType("RETELL_AGENT")
                .isActive(true)
                .build();
        validRetellKey.setId(10L);

        clinic = ClinicDetails.builder()
                .clinicId(1L)
                .clinicName("Smile Dental Clinic")
                .build();
        clinic.setId(1L);
    }

    @Test
    @DisplayName("Valid Retell API key should authenticate, establish Retell context, and proceed")
    void doFilterInternal_withValidRetellApiKey_shouldAuthenticate() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/appointments");
        request.addHeader("X-API-KEY", "retell_live_clinic_1_sk_sample_key_2026");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> {
            // Verify context during filter execution
            assertThat(ClinicRequestContext.isAuthenticated()).isTrue();
            assertThat(ClinicRequestContext.getClinicId()).isEqualTo(1L);
            assertThat(ClinicRequestContext.getSource()).isEqualTo(RequestSource.RETELL_AI_AGENT);
            assertThat(ClinicRequestContext.isRetellAiAgent()).isTrue();
            assertThat(ClinicRequestContext.getBookingSource()).isEqualTo(BookingSource.AI_AGENT);
        };

        when(apiKeyService.validateAndAuthenticateKey(eq("retell_live_clinic_1_sk_sample_key_2026"), any()))
                .thenReturn(validRetellKey);
        when(clinicDetailsRepository.findByClinicIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(clinic));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        // Verify context is cleared afterwards
        assertThat(ClinicRequestContext.getContext()).isNull();
    }

    @Test
    @DisplayName("Invalid or inactive API key should return 401 Unauthorized")
    void doFilterInternal_withInvalidApiKey_shouldReturn401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/appointments");
        request.addHeader("X-API-KEY", "invalid_key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> {};

        when(apiKeyService.validateAndAuthenticateKey(eq("invalid_key"), any()))
                .thenThrow(new InvalidCredentialsException("Invalid or inactive API key"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid or inactive API key");
    }

    @Test
    @DisplayName("Non-Retell agent attempting to use API key should return 403 Forbidden")
    void doFilterInternal_withNonRetellAgent_shouldReturn403() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/appointments");
        request.addHeader("X-API-KEY", "retell_live_clinic_1_sk_sample_key_2026");
        request.addHeader("X-Agent-Type", "OTHER_AGENT");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> {};

        when(apiKeyService.validateAndAuthenticateKey(eq("retell_live_clinic_1_sk_sample_key_2026"), eq("OTHER_AGENT")))
                .thenThrow(new SecurityException("Access denied: API key is allowed only for Retell agent"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("allowed only for Retell agent");
    }

    @Test
    @DisplayName("Attempting to access as Retell agent without API key should return 401 Unauthorized")
    void doFilterInternal_claimingRetellWithoutKey_shouldReturn401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/appointments");
        request.addHeader("X-Request-Source", "RETELL_AI_AGENT");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> {};

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Retell agent requests must authenticate using a valid API key");
    }

    @Test
    @DisplayName("OPTIONS request should bypass authentication")
    void doFilterInternal_optionsRequest_shouldBypass() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/appointments");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> ((HttpServletResponse) res).setStatus(200);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(apiKeyService, never()).validateAndAuthenticateKey(any(), any());
    }
}
