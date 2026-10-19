package com.dental.manage.service;

import com.dental.manage.service.impl.JwtTokenServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private JwtTokenServiceImpl jwtTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1kZW50YWwtY2xpbmljLW1hbmFnZW1lbnQtc3lzdGVtLTI1Ng==";

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenServiceImpl(objectMapper, SECRET, 86400000L);
    }

    @Test
    @DisplayName("createToken should generate valid 3-part JWT token with 1-day validity")
    void createToken_shouldGenerateValidToken() {
        String token = jwtTokenService.createToken("receptionist", "RECEPTIONIST");

        assertThat(token).isNotNull();
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);

        boolean isValid = jwtTokenService.validateToken(token);
        assertThat(isValid).isTrue();

        String username = jwtTokenService.extractUsername(token);
        assertThat(username).isEqualTo("receptionist");

        String role = jwtTokenService.extractRole(token);
        assertThat(role).isEqualTo("RECEPTIONIST");

        Date expiration = jwtTokenService.extractExpiration(token);
        assertThat(expiration).isNotNull();
        long diffMs = expiration.getTime() - System.currentTimeMillis();
        // Expiration should be close to 1 day (within 5 seconds tolerance)
        assertThat(diffMs).isBetween(86400000L - 5000L, 86400000L + 1000L);
    }

    @Test
    @DisplayName("createToken with extra claims should include custom fields")
    void createToken_withExtraClaims_shouldIncludeClaims() {
        Map<String, Object> extra = Map.of(
                "clinicId", 1L,
                "doctorId", 42L,
                "name", "Dr. Jane Smith"
        );

        String token = jwtTokenService.createToken("doctor", "DOCTOR", extra);

        assertThat(jwtTokenService.validateToken(token)).isTrue();
        Map<String, Object> claims = jwtTokenService.extractAllClaims(token);
        assertThat(claims.get("sub")).isEqualTo("doctor");
        assertThat(claims.get("role")).isEqualTo("DOCTOR");
        assertThat(claims.get("name")).isEqualTo("Dr. Jane Smith");
    }

    @Test
    @DisplayName("validateToken should return false for tampered token")
    void validateToken_shouldReturnFalseForTamperedToken() {
        String token = jwtTokenService.createToken("doctor", "DOCTOR");
        String tampered = token.substring(0, token.length() - 5) + "ABCDE";

        assertThat(jwtTokenService.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken should return false for null, empty or malformed tokens")
    void validateToken_shouldReturnFalseForInvalidInput() {
        assertThat(jwtTokenService.validateToken(null)).isFalse();
        assertThat(jwtTokenService.validateToken("")).isFalse();
        assertThat(jwtTokenService.validateToken("header.payload")).isFalse();
    }

    @Test
    @DisplayName("token validity must be capped at a maximum of 1 day")
    void tokenValidity_shouldBeCappedAtOneDay() {
        // Attempt to configure 7 days (604,800,000 ms)
        JwtTokenServiceImpl cappedService = new JwtTokenServiceImpl(objectMapper, SECRET, 7 * 24 * 60 * 60 * 1000L);
        assertThat(cappedService.getValidityInMilliseconds()).isEqualTo(86400000L);
    }

    @Test
    @DisplayName("validateToken should return false for expired token")
    void validateToken_shouldReturnFalseForExpiredToken() {
        // Service configured with negative validity to simulate expired token
        JwtTokenServiceImpl expiredService = new JwtTokenServiceImpl(objectMapper, SECRET, -1000L);
        String token = expiredService.createToken("receptionist", "RECEPTIONIST");

        assertThat(expiredService.validateToken(token)).isFalse();
        assertThat(expiredService.isTokenExpired(token)).isTrue();
    }
}
