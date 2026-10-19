package com.dental.manage.controller;

import com.dental.manage.dto.request.LoginRequest;
import com.dental.manage.dto.response.LoginResponse;
import com.dental.manage.exception.AccessDeniedException;
import com.dental.manage.exception.GlobalExceptionHandler;
import com.dental.manage.exception.InvalidCredentialsException;
import com.dental.manage.service.JWTtokenService;
import com.dental.manage.service.UserAuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserAuthenticationService userAuthenticationService;

    @Mock
    private JWTtokenService jwtTokenService;

    @InjectMocks
    private LoginController loginController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(loginController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/login should authenticate RECEPTIONIST and return JWT token")
    void login_receptionist_shouldReturnToken() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .token("jwt.receptionist.token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .username("receptionist")
                .role("RECEPTIONIST")
                .fullName("Clinic Receptionist")
                .clinicId(1L)
                .build();

        when(userAuthenticationService.authenticate(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = LoginRequest.builder()
                .username(encode("receptionist"))
                .password(encode("reception123"))
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", is("jwt.receptionist.token")))
                .andExpect(jsonPath("$.data.role", is("RECEPTIONIST")))
                .andExpect(jsonPath("$.data.username", is("receptionist")))
                .andExpect(jsonPath("$.data.expiresIn", is(86400)));
    }

    @Test
    @DisplayName("POST /api/login alias should authenticate DOCTOR and return JWT token")
    void login_doctor_alias_shouldReturnToken() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .token("jwt.doctor.token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .username("dr.smith")
                .role("DOCTOR")
                .fullName("Dr. Jane Smith")
                .id(10L)
                .build();

        when(userAuthenticationService.authenticate(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = LoginRequest.builder()
                .username(encode("dr.smith"))
                .password(encode("doctor123"))
                .build();

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", is("jwt.doctor.token")))
                .andExpect(jsonPath("$.data.role", is("DOCTOR")))
                .andExpect(jsonPath("$.data.id", is(10)));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 403 Forbidden when patient attempts to log in")
    void login_patient_shouldReturnForbidden() throws Exception {
        when(userAuthenticationService.authenticate(any(LoginRequest.class)))
                .thenThrow(new AccessDeniedException("Access denied. Patients have no access to the portal as of now."));

        LoginRequest request = LoginRequest.builder()
                .username(encode("patient"))
                .password(encode("patient123"))
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Access denied. Patients have no access to the portal as of now.")));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 401 Unauthorized for invalid credentials")
    void login_invalidCredentials_shouldReturnUnauthorized() throws Exception {
        when(userAuthenticationService.authenticate(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid username or password"));

        LoginRequest request = LoginRequest.builder()
                .username(encode("receptionist"))
                .password(encode("wrongpassword"))
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid username or password")));
    }

    @Test
    @DisplayName("GET /api/auth/validate should return 200 OK for valid Bearer token")
    void validateToken_validToken_shouldReturnOk() throws Exception {
        String token = "valid.jwt.token";
        when(jwtTokenService.validateToken(token)).thenReturn(true);
        when(jwtTokenService.extractUsername(token)).thenReturn("receptionist");
        when(jwtTokenService.extractRole(token)).thenReturn("RECEPTIONIST");

        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.valid", is(true)))
                .andExpect(jsonPath("$.data.username", is("receptionist")))
                .andExpect(jsonPath("$.data.role", is("RECEPTIONIST")));
    }

    @Test
    @DisplayName("GET /api/auth/validate should return 401 Unauthorized for invalid Bearer token")
    void validateToken_invalidToken_shouldReturnUnauthorized() throws Exception {
        String token = "expired.jwt.token";
        when(jwtTokenService.validateToken(token)).thenReturn(false);

        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Token is invalid or expired")));
    }

    @Test
    @DisplayName("GET /api/auth/validate should return 401 Unauthorized when Authorization header is missing")
    void validateToken_missingHeader_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Missing or malformed Authorization header")));
    }
}
