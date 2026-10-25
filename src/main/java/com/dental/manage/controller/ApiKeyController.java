package com.dental.manage.controller;

import com.dental.manage.dto.response.ApiKeyResponse;
import com.dental.manage.dto.response.ApiResponse;
import com.dental.manage.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for managing API keys designated exclusively for the Retell AI Voice Agent.
 */
@RestController
@RequestMapping({"/api/api-keys", "/api/keys"})
@RequiredArgsConstructor
@Slf4j
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Retrieves the active Retell API key for the clinic.
     *
     * @param clinicId optional clinicId query param
     * @return active ApiKeyResponse
     */
    @GetMapping({"", "/active"})
    public ResponseEntity<ApiResponse<ApiKeyResponse>> getActiveRetellApiKey(
            @RequestParam(name = "clinicId", required = false) Long clinicId) {
        log.info("Request received to fetch active Retell API key (clinicId={})", clinicId);
        ApiKeyResponse response = apiKeyService.getActiveRetellApiKey(clinicId);
        return ResponseEntity.ok(ApiResponse.success("Active Retell API key retrieved successfully", response));
    }

    /**
     * Regenerates a new active Retell API key for the clinic, deactivating previous keys.
     *
     * @param clinicId optional clinicId query param
     * @param keyName optional key name description
     * @return newly generated ApiKeyResponse
     */
    @PostMapping({"/regenerate", ""})
    public ResponseEntity<ApiResponse<ApiKeyResponse>> regenerateRetellApiKey(
            @RequestParam(name = "clinicId", required = false) Long clinicId,
            @RequestParam(name = "name", required = false) String keyName) {
        log.info("Request received to regenerate Retell API key (clinicId={}, name={})", clinicId, keyName);
        ApiKeyResponse response = apiKeyService.createOrRegenerateRetellApiKey(clinicId, keyName);
        return ResponseEntity.ok(ApiResponse.success("Retell API key regenerated successfully", response));
    }

    /**
     * Lists all API keys for the clinic.
     *
     * @param clinicId optional clinicId query param
     * @return list of ApiKeyResponse
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> getAllClinicApiKeys(
            @RequestParam(name = "clinicId", required = false) Long clinicId) {
        log.info("Request received to fetch all API keys (clinicId={})", clinicId);
        List<ApiKeyResponse> response = apiKeyService.getClinicApiKeys(clinicId);
        return ResponseEntity.ok(ApiResponse.success("Clinic API keys retrieved successfully", response));
    }

    /**
     * Deactivates a specific API key.
     *
     * @param id key ID
     * @param clinicId optional clinicId
     * @return success response
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateApiKey(
            @PathVariable Long id,
            @RequestParam(name = "clinicId", required = false) Long clinicId) {
        log.info("Request received to deactivate API key id={} (clinicId={})", id, clinicId);
        apiKeyService.deactivateApiKey(id, clinicId);
        return ResponseEntity.ok(ApiResponse.success("API key deactivated successfully", null));
    }

    /**
     * Soft deletes a specific API key.
     *
     * @param id key ID
     * @param clinicId optional clinicId
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(
            @PathVariable Long id,
            @RequestParam(name = "clinicId", required = false) Long clinicId) {
        log.info("Request received to delete API key id={} (clinicId={})", id, clinicId);
        apiKeyService.deleteApiKey(id, clinicId);
        return ResponseEntity.ok(ApiResponse.success("API key deleted successfully", null));
    }
}
