package com.dental.manage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing an API key configured for a clinic and restricted exclusively to the Retell agent.
 */
@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey extends BaseEntity {

    @Column(name = "clinic_id", nullable = false)
    private Long clinicId;

    @Column(name = "key_name", nullable = false, length = 100)
    private String keyName;

    @Column(name = "api_key", nullable = false, unique = true, length = 255)
    private String apiKey;

    @Column(name = "agent_type", nullable = false, length = 50)
    @Builder.Default
    private String agentType = "RETELL_AGENT";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * Helper to verify if this API key is designated for the Retell agent.
     */
    public boolean isRetellAgentKey() {
        if (agentType == null) {
            return false;
        }
        String normalized = agentType.trim().toUpperCase();
        return normalized.contains("RETELL");
    }

    /**
     * Helper to verify if the key is currently usable (active, not deleted, and not expired).
     */
    public boolean isUsable() {
        if (Boolean.TRUE.equals(getIsDeleted()) || !Boolean.TRUE.equals(isActive)) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(LocalDateTime.now());
    }
}
