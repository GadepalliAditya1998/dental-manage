package com.dental.manage.service.impl;

import com.dental.manage.service.JWTtokenService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard HMAC-SHA256 (RFC 7519) JSON Web Token service implementation.
 * Enforces a maximum token validity of 1 day (86,400,000 milliseconds).
 */
@Service("jwtTokenService")
@Slf4j
public class JwtTokenServiceImpl implements JWTtokenService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    public static final long ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L; // 86,400,000 ms

    private static final String DEFAULT_HEADER = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final ObjectMapper objectMapper;
    private final byte[] secretKeyBytes;
    private final long validityInMillis;

    public JwtTokenServiceImpl(
            ObjectMapper objectMapper,
            @Value("${jwt.secret:dGVzdC1zZWNyZXQta2V5LWZvci1kZW50YWwtY2xpbmljLW1hbmFnZW1lbnQtc3lzdGVtLTI1Ng==}") String secretKey,
            @Value("${jwt.expiration-ms:86400000}") long configuredValidity) {
        this.objectMapper = objectMapper;
        this.secretKeyBytes = resolveSecretKey(secretKey);
        // Enforce max 1 day validity
        this.validityInMillis = Math.min(configuredValidity, ONE_DAY_IN_MILLIS);
        log.info("JWTtokenService initialized with validity of {} ms (max 1 day)", this.validityInMillis);
    }

    @Override
    public String createToken(String username, String role) {
        return createToken(username, role, Map.of());
    }

    @Override
    public String createToken(String username, String role, Map<String, Object> extraClaims) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty when creating JWT token");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be empty when creating JWT token");
        }

        Instant now = Instant.now();
        Instant expiry = now.plusMillis(validityInMillis);

        Map<String, Object> payload = new HashMap<>();
        if (extraClaims != null) {
            payload.putAll(extraClaims);
        }
        payload.put("sub", username);
        payload.put("role", role);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiry.getEpochSecond());

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));

            String contentToSign = DEFAULT_HEADER + "." + encodedPayload;
            String signature = sign(contentToSign);

            return contentToSign + "." + signature;
        } catch (Exception e) {
            log.error("Failed to serialize JWT payload", e);
            throw new IllegalStateException("Failed to generate JWT token", e);
        }
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            log.warn("Invalid JWT format: token must have 3 parts separated by dots");
            return false;
        }

        try {
            String contentToSign = parts[0] + "." + parts[1];
            byte[] expectedSig = computeHmac(contentToSign);
            byte[] providedSig = Base64.getUrlDecoder().decode(parts[2]);

            if (!MessageDigest.isEqual(expectedSig, providedSig)) {
                log.warn("JWT token signature mismatch");
                return false;
            }

            if (isTokenExpired(token)) {
                log.warn("JWT token has expired");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String extractUsername(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        Object sub = claims.get("sub");
        return sub != null ? sub.toString() : null;
    }

    @Override
    public String extractRole(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        Object role = claims.get("role");
        return role != null ? role.toString() : null;
    }

    @Override
    public Date extractExpiration(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        Object expObj = claims.get("exp");
        if (expObj instanceof Number num) {
            return new Date(num.longValue() * 1000L);
        }
        return null;
    }

    @Override
    public boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration == null || expiration.before(new Date());
    }

    @Override
    public long getValidityInMilliseconds() {
        return validityInMillis;
    }

    @Override
    public Map<String, Object> extractAllClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Malformed JWT token structure");
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JWT payload: " + e.getMessage(), e);
        }
    }

    private String sign(String content) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] signatureBytes = computeHmac(content);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
    }

    private byte[] computeHmac(String content) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, HMAC_SHA256);
        mac.init(keySpec);
        return mac.doFinal(content.getBytes(StandardCharsets.US_ASCII));
    }

    private byte[] resolveSecretKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            rawKey = "dGVzdC1zZWNyZXQta2V5LWZvci1kZW50YWwtY2xpbmljLW1hbmFnZW1lbnQtc3lzdGVtLTI1Ng==";
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(rawKey.trim());
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Not base64 encoded, use raw bytes
        }
        byte[] utf8Bytes = rawKey.getBytes(StandardCharsets.UTF_8);
        if (utf8Bytes.length < 32) {
            // Pad to 32 bytes for HMAC-SHA256 standard security
            byte[] padded = new byte[32];
            System.arraycopy(utf8Bytes, 0, padded, 0, utf8Bytes.length);
            return padded;
        }
        return utf8Bytes;
    }
}
