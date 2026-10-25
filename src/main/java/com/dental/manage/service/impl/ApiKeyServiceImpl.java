package com.dental.manage.service.impl;

import com.dental.manage.context.ClinicRequestContext;
import com.dental.manage.dto.response.ApiKeyResponse;
import com.dental.manage.entity.ApiKey;
import com.dental.manage.entity.ClinicDetails;
import com.dental.manage.enums.AgentType;
import com.dental.manage.exception.InvalidCredentialsException;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.repository.ApiKeyRepository;
import com.dental.manage.repository.ClinicDetailsRepository;
import com.dental.manage.service.ApiKeyService;
import com.dental.manage.util.ApiKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ApiKeyService managing dedicated API keys restricted strictly
 * to the Retell AI Voice Agent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyServiceImpl implements ApiKeyService {

    public static final String RETELL_AGENT_TYPE = "RETELL_AGENT";
    public static final String DEFAULT_RETELL_KEY_NAME = "Retell AI Voice Agent";

    private final ApiKeyRepository apiKeyRepository;
    private final ClinicDetailsRepository clinicDetailsRepository;

    @Override
    @Transactional
    public ApiKey validateAndAuthenticateKey(String apiKey, String callerAgentOrSource) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new InvalidCredentialsException("API key must not be blank");
        }

        String trimmedKey = apiKey.trim();

        // 1. Locate API key in the dedicated api_keys table
        ApiKey keyEntity = apiKeyRepository.findByApiKeyAndIsDeletedFalse(trimmedKey)
                .orElseGet(() -> migrateFromClinicDetailsIfPresent(trimmedKey)
                        .orElseThrow(() -> new InvalidCredentialsException("Invalid or inactive API key")));

        // 2. Validate active state and expiration
        if (!Boolean.TRUE.equals(keyEntity.getIsActive())) {
            log.warn("Authentication failed: API key is inactive (id={}, clinicId={})", keyEntity.getId(), keyEntity.getClinicId());
            throw new InvalidCredentialsException("API key is inactive");
        }

        if (keyEntity.getExpiresAt() != null && keyEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Authentication failed: API key has expired (id={}, clinicId={})", keyEntity.getId(), keyEntity.getClinicId());
            throw new InvalidCredentialsException("API key has expired");
        }

        // 3. Strictly enforce: API key must be allowed ONLY for the Retell agent
        if (!keyEntity.isRetellAgentKey()) {
            log.warn("Access denied: API key agentType '{}' is not permitted. Only Retell agent is allowed.", keyEntity.getAgentType());
            throw new SecurityException("Access denied: API key is allowed only for Retell agent");
        }

        // 4. Validate caller identity header if explicitly provided
        if (callerAgentOrSource != null && !callerAgentOrSource.isBlank()) {
            if (!AgentType.isRetellAgent(callerAgentOrSource)) {
                log.warn("Access denied: caller claimed to be non-Retell agent/source '{}' using Retell API key", callerAgentOrSource);
                throw new SecurityException("Access denied: API key is allowed only for Retell agent");
            }
        }

        // 5. Update last used timestamp
        keyEntity.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(keyEntity);

        log.debug("Successfully validated Retell API key for clinicId={}", keyEntity.getClinicId());
        return keyEntity;
    }

    @Override
    @Transactional
    public ApiKeyResponse getActiveRetellApiKey(Long clinicId) {
        ClinicDetails clinic = resolveClinic(clinicId);
        Long resolvedClinicId = clinic.getClinicId() != null ? clinic.getClinicId() : clinic.getId();

        Optional<ApiKey> activeKeyOpt = apiKeyRepository
                .findFirstByClinicIdAndAgentTypeAndIsActiveTrueAndIsDeletedFalseOrderByCreatedOnDesc(
                        resolvedClinicId, RETELL_AGENT_TYPE);

        if (activeKeyOpt.isPresent()) {
            return toResponse(activeKeyOpt.get(), clinic.getClinicName(), "Clinic API key retrieved successfully");
        }

        log.info("No active Retell API key found for clinicId={}. Generating new key...", resolvedClinicId);
        return createOrRegenerateRetellApiKey(resolvedClinicId, DEFAULT_RETELL_KEY_NAME);
    }

    @Override
    @Transactional
    public ApiKeyResponse createOrRegenerateRetellApiKey(Long clinicId, String keyName) {
        ClinicDetails clinic = resolveClinic(clinicId);
        Long resolvedClinicId = clinic.getClinicId() != null ? clinic.getClinicId() : clinic.getId();

        // Deactivate existing active keys for this clinic and agent
        apiKeyRepository.deactivateAllForClinicAndAgent(resolvedClinicId, RETELL_AGENT_TYPE);

        String newKey = ApiKeyGenerator.generateApiKey();
        String effectiveKeyName = (keyName != null && !keyName.isBlank()) ? keyName.trim() : DEFAULT_RETELL_KEY_NAME;

        ApiKey newApiKey = ApiKey.builder()
                .clinicId(resolvedClinicId)
                .keyName(effectiveKeyName)
                .apiKey(newKey)
                .agentType(RETELL_AGENT_TYPE)
                .isActive(true)
                .build();

        ApiKey saved = apiKeyRepository.save(newApiKey);

        // Keep clinic_details.api_key synchronized
        clinic.setApiKey(newKey);
        clinicDetailsRepository.save(clinic);

        log.info("Successfully generated new Retell API key (id={}) for clinic '{}' (clinicId={})",
                saved.getId(), clinic.getClinicName(), resolvedClinicId);

        return toResponse(saved, clinic.getClinicName(), "API key regenerated successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getClinicApiKeys(Long clinicId) {
        ClinicDetails clinic = resolveClinic(clinicId);
        Long resolvedClinicId = clinic.getClinicId() != null ? clinic.getClinicId() : clinic.getId();

        List<ApiKey> keys = apiKeyRepository.findByClinicIdAndIsDeletedFalseOrderByCreatedOnDesc(resolvedClinicId);
        return keys.stream()
                .map(k -> toResponse(k, clinic.getClinicName(), null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateApiKey(Long keyId, Long clinicId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found with id: " + keyId));

        if (clinicId != null && !clinicId.equals(apiKey.getClinicId())) {
            throw new SecurityException("API key does not belong to specified clinic");
        }

        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
        log.info("Deactivated API key id={} for clinicId={}", keyId, apiKey.getClinicId());
    }

    @Override
    @Transactional
    public void deleteApiKey(Long keyId, Long clinicId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found with id: " + keyId));

        if (clinicId != null && !clinicId.equals(apiKey.getClinicId())) {
            throw new SecurityException("API key does not belong to specified clinic");
        }

        apiKey.setIsDeleted(true);
        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
        log.info("Soft deleted API key id={} for clinicId={}", keyId, apiKey.getClinicId());
    }

    private Optional<ApiKey> migrateFromClinicDetailsIfPresent(String apiKey) {
        Optional<ClinicDetails> clinicOpt = clinicDetailsRepository.findByApiKeyAndIsDeletedFalse(apiKey);
        if (clinicOpt.isPresent()) {
            ClinicDetails clinic = clinicOpt.get();
            Long clinicId = clinic.getClinicId() != null ? clinic.getClinicId() : clinic.getId();
            log.info("Migrating legacy API key from clinic_details to api_keys table for clinicId={}", clinicId);

            ApiKey migrated = ApiKey.builder()
                    .clinicId(clinicId)
                    .keyName("Migrated Clinic Key")
                    .apiKey(apiKey)
                    .agentType(RETELL_AGENT_TYPE)
                    .isActive(true)
                    .build();
            return Optional.of(apiKeyRepository.save(migrated));
        }
        return Optional.empty();
    }

    private ClinicDetails resolveClinic(Long clinicId) {
        if (clinicId != null) {
            return clinicDetailsRepository.findByClinicIdAndIsDeletedFalse(clinicId)
                    .orElseGet(this::getSingletonClinicDetails);
        }
        Long contextClinicId = ClinicRequestContext.getClinicId();
        if (contextClinicId != null) {
            return clinicDetailsRepository.findByClinicIdAndIsDeletedFalse(contextClinicId)
                    .orElseGet(this::getSingletonClinicDetails);
        }
        return getSingletonClinicDetails();
    }

    private ClinicDetails getSingletonClinicDetails() {
        return clinicDetailsRepository.findFirstByIsDeletedFalseOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("No active clinic found in the system"));
    }

    private ApiKeyResponse toResponse(ApiKey entity, String clinicName, String message) {
        return ApiKeyResponse.builder()
                .id(entity.getId())
                .clinicId(entity.getClinicId())
                .clinicName(clinicName)
                .keyName(entity.getKeyName())
                .apiKey(entity.getApiKey())
                .agentType(entity.getAgentType())
                .isActive(entity.getIsActive())
                .message(message)
                .generatedAt(entity.getCreatedOn())
                .expiresAt(entity.getExpiresAt())
                .lastUsedAt(entity.getLastUsedAt())
                .build();
    }
}
