package com.dental.manage.controller;

import com.dental.manage.dto.request.LoginRequest;
import com.dental.manage.dto.response.ApiResponse;
import com.dental.manage.dto.response.LoginResponse;
import com.dental.manage.service.JWTtokenService;
import com.dental.manage.service.UserAuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for user authentication and token verification.
 * Accepts credentials in base64 format and issues 1-day JWT tokens
 * for authorized roles (RECEPTIONIST, DOCTOR).
 */
@RestController
@RequestMapping({"/api/auth", "/api"})
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final UserAuthenticationService userAuthenticationService;
    private final JWTtokenService jwtTokenService;

    /**
     * Authenticates a user using base64-encoded username and password.
     * Only RECEPTIONIST and DOCTOR roles are permitted.
     *
     * @param request LoginRequest containing base64 encoded username and password
     * @return ApiResponse containing LoginResponse with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Received login request");
        LoginResponse response = userAuthenticationService.authenticate(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Validates an existing JWT token passed in the Authorization header.
     *
     * @param authHeader Bearer token header
     * @return ApiResponse indicating validity and claims
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Missing or malformed Authorization header"));
        }

        String token = authHeader.substring(7).trim();
        boolean isValid = jwtTokenService.validateToken(token);

        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token is invalid or expired"));
        }

        String username = jwtTokenService.extractUsername(token);
        String role = jwtTokenService.extractRole(token);
        Map<String, Object> claims = jwtTokenService.extractAllClaims(token);

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("valid", true);
        data.put("username", username != null ? username : "");
        data.put("role", role != null ? role : "");
        if (claims != null) {
            if (claims.get("doctorId") != null) {
                data.put("doctorId", claims.get("doctorId"));
            }
            if (claims.get("clinicId") != null) {
                data.put("clinicId", claims.get("clinicId"));
            }
            if (claims.get("name") != null) {
                data.put("fullName", claims.get("name"));
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Token is valid", data));
    }
}
