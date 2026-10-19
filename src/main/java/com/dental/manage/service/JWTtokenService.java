package com.dental.manage.service;

import java.util.Date;
import java.util.Map;

/**
 * Service for generating and validating JSON Web Tokens (JWT).
 * Generates tokens with a maximum validity of 1 day (86,400,000 milliseconds).
 */
public interface JWTtokenService {

    /**
     * Creates a signed JWT token for the specified user and role with 1-day validity.
     *
     * @param username user identifier
     * @param role user role (e.g., RECEPTIONIST, DOCTOR)
     * @return signed JWT token string
     */
    String createToken(String username, String role);

    /**
     * Creates a signed JWT token with additional custom claims and 1-day validity.
     *
     * @param username user identifier
     * @param role user role
     * @param extraClaims map of additional claims (e.g., fullName, clinicId, doctorId)
     * @return signed JWT token string
     */
    String createToken(String username, String role, Map<String, Object> extraClaims);

    /**
     * Validates the integrity, signature, and expiration of the provided JWT token.
     *
     * @param token JWT token string
     * @return true if valid and not expired; false otherwise
     */
    boolean validateToken(String token);

    /**
     * Extracts the subject (username) from the token.
     *
     * @param token JWT token string
     * @return username subject
     */
    String extractUsername(String token);

    /**
     * Extracts the role claim from the token.
     *
     * @param token JWT token string
     * @return role string
     */
    String extractRole(String token);

    /**
     * Extracts the expiration date from the token.
     *
     * @param token JWT token string
     * @return expiration Date
     */
    Date extractExpiration(String token);

    /**
     * Checks if the token is expired.
     *
     * @param token JWT token string
     * @return true if expired; false otherwise
     */
    boolean isTokenExpired(String token);

    /**
     * Returns the token validity in milliseconds (max 1 day: 86,400,000 ms).
     *
     * @return validity duration in ms
     */
    long getValidityInMilliseconds();

    /**
     * Extracts all claims from the JWT payload.
     *
     * @param token JWT token string
     * @return map of claims
     */
    Map<String, Object> extractAllClaims(String token);
}
