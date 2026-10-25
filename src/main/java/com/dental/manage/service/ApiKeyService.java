package com.dental.manage.service;

import com.dental.manage.dto.response.ApiKeyResponse;
import com.dental.manage.entity.ApiKey;

import java.util.List;

/**
 * Service interface for managing clinic API keys restricted exclusively to the Retell agent.
 */
public interface ApiKeyService {

    /**
     * Authenticates and validates an incoming API key string, strictly enforcing that
     * the key is active and restricted only to the Retell agent.
     *
     * @param apiKey raw API key string
     * @param callerAgentOrSource optional caller agent identifier (e.g. from header)
     * @return validated ApiKey entity
     */
    ApiKey validateAndAuthenticateKey(String apiKey, String callerAgentOrSource);

    /**
     * Retrieves the active Retell API key for the given clinic.
     * Automatically generates a new one if no active key exists.
     *
     * @param clinicId target clinic ID
     * @return ApiKeyResponse DTO
     */
    ApiKeyResponse getActiveRetellApiKey(Long clinicId);

    /**
     * Regenerates a new active Retell API key for the given clinic, deactivating any prior keys.
     *
     * @param clinicId target clinic ID
     * @param keyName optional custom name for the key
     * @return ApiKeyResponse DTO with the new key
     */
    ApiKeyResponse createOrRegenerateRetellApiKey(Long clinicId, String keyName);

    /**
     * Lists all non-deleted API keys for a specific clinic.
     *
     * @param clinicId target clinic ID
     * @return list of ApiKeyResponse
     */
    List<ApiKeyResponse> getClinicApiKeys(Long clinicId);

    /**
     * Deactivates a specific API key.
     *
     * @param keyId key ID
     * @param clinicId optional clinic ID scoping
     */
    void deactivateApiKey(Long keyId, Long clinicId);

    /**
     * Soft deletes a specific API key.
     *
     * @param keyId key ID
     * @param clinicId optional clinic ID scoping
     */
    void deleteApiKey(Long keyId, Long clinicId);
}
