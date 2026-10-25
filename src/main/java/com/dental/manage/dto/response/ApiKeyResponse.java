package com.dental.manage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for clinic API key operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyResponse {

    private Long id;
    private Long clinicId;
    private String clinicName;
    private String keyName;
    private String apiKey;
    private String agentType;
    private Boolean isActive;
    private String message;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
}
