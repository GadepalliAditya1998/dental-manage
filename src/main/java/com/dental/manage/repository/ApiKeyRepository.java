package com.dental.manage.repository;

import com.dental.manage.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for managing API key entities and lookups.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByApiKeyAndIsDeletedFalse(String apiKey);

    Optional<ApiKey> findByApiKeyAndIsActiveTrueAndIsDeletedFalse(String apiKey);

    List<ApiKey> findByClinicIdAndIsDeletedFalseOrderByCreatedOnDesc(Long clinicId);

    Optional<ApiKey> findFirstByClinicIdAndAgentTypeAndIsActiveTrueAndIsDeletedFalseOrderByCreatedOnDesc(
            Long clinicId, String agentType);

    Optional<ApiKey> findFirstByClinicIdAndIsActiveTrueAndIsDeletedFalseOrderByCreatedOnDesc(Long clinicId);

    boolean existsByApiKey(String apiKey);

    @Modifying
    @Query("UPDATE ApiKey k SET k.isActive = false, k.modifiedOn = CURRENT_TIMESTAMP WHERE k.clinicId = :clinicId AND k.agentType = :agentType AND k.isDeleted = false")
    void deactivateAllForClinicAndAgent(@Param("clinicId") Long clinicId, @Param("agentType") String agentType);

    @Modifying
    @Query("UPDATE ApiKey k SET k.lastUsedAt = :timestamp WHERE k.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("timestamp") LocalDateTime timestamp);
}
