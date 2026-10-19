package com.dental.manage.service.impl;

import com.dental.manage.dto.request.LoginRequest;
import com.dental.manage.dto.response.LoginResponse;
import com.dental.manage.entity.User;
import com.dental.manage.enums.UserRole;
import com.dental.manage.exception.AccessDeniedException;
import com.dental.manage.exception.InvalidCredentialsException;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.service.JWTtokenService;
import com.dental.manage.service.UserAuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of UserAuthenticationService.
 * Decodes base64-encoded credentials, validates role permissions,
 * and generates 1-day valid JWT tokens for RECEPTIONIST and DOCTOR users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserAuthenticationServiceImpl implements UserAuthenticationService {

    private final UserRepository userRepository;
    private final JWTtokenService jwtTokenService;

    @Override
    public LoginResponse authenticate(LoginRequest request) {
        String decodedUsername = decodeBase64(request.getUsername(), "Username");
        String decodedPassword = decodeBase64(request.getPassword(), "Password");

        log.info("Attempting authentication for user: {}", decodedUsername);

        User user = userRepository.findByUsernameAndIsDeletedFalse(decodedUsername)
                .or(() -> userRepository.findByEmailAndIsDeletedFalse(decodedUsername))
                .orElseGet(() -> null);

        if (user == null) {
            log.warn("Authentication failed: user '{}' not found", decodedUsername);
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (!user.getPassword().equals(decodedPassword)) {
            log.warn("Authentication failed: invalid password for user '{}'", decodedUsername);
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            log.warn("Authentication failed: user account '{}' is disabled", decodedUsername);
            throw new InvalidCredentialsException("User account is disabled");
        }

        // Only RECEPTIONIST and DOCTOR roles are permitted to log in to the portal
        if (!user.getRole().isPortalAccessAllowed()) {
            log.warn("Portal access rejected: user '{}' has role '{}'", decodedUsername, user.getRole());
            throw new AccessDeniedException("Access denied. Patients have no access to the portal as of now.");
        }

        Map<String, Object> extraClaims = new HashMap<>();
        if (user.getId() != null) {
            extraClaims.put("userId", user.getId());
        }
        if (user.getFullName() != null) {
            extraClaims.put("name", user.getFullName());
        }
        if (user.getClinicId() != null) {
            extraClaims.put("clinicId", user.getClinicId());
        }

        String token = jwtTokenService.createToken(user.getUsername(), user.getRole().name(), extraClaims);
        long expiresInSeconds = jwtTokenService.getValidityInMilliseconds() / 1000L;

        log.info("Authentication successful for user '{}' with role '{}'", user.getUsername(), user.getRole());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .clinicId(user.getClinicId())
                .build();
    }

    private String decodeBase64(String encoded, String fieldName) {
        if (encoded == null || encoded.isBlank()) {
            throw new InvalidCredentialsException(fieldName + " is required");
        }
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encoded.trim());
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Base64 formatting for {}", fieldName);
            throw new InvalidCredentialsException("Invalid credentials format: " + fieldName + " must be base64-encoded");
        }
    }

    private User findDefaultUser(String username) {
        if ("receptionist".equalsIgnoreCase(username)) {
            return User.builder()
                    .clinicId(1L)
                    .username("receptionist")
                    .password("reception123")
                    .role(UserRole.RECEPTIONIST)
                    .fullName("Clinic Receptionist")
                    .isActive(true)
                    .build();
        }
        if ("doctor".equalsIgnoreCase(username)) {
            return User.builder()
                    .clinicId(1L)
                    .username("doctor")
                    .password("doctor123")
                    .role(UserRole.DOCTOR)
                    .fullName("Dr. Jane Smith")
                    .isActive(true)
                    .build();
        }
        if ("patient".equalsIgnoreCase(username)) {
            return User.builder()
                    .clinicId(1L)
                    .username("patient")
                    .password("patient123")
                    .role(UserRole.PATIENT)
                    .fullName("John Doe")
                    .isActive(true)
                    .build();
        }
        return null;
    }
}
