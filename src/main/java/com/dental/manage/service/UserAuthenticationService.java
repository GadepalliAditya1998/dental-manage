package com.dental.manage.service;

import com.dental.manage.dto.request.LoginRequest;
import com.dental.manage.dto.response.LoginResponse;

/**
 * Service for handling user authentication and portal access verification.
 */
public interface UserAuthenticationService {

    /**
     * Authenticates a user with base64-encoded username and password.
     * Enforces that only RECEPTIONIST and DOCTOR roles are permitted portal access.
     *
     * @param request LoginRequest containing base64 encoded credentials
     * @return LoginResponse containing signed JWT token and user info
     */
    LoginResponse authenticate(LoginRequest request);
}
