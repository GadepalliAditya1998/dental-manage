package com.dental.manage.repository;

import com.dental.manage.entity.ClinicDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicDetailsRepository extends JpaRepository<ClinicDetails, Long> {

    /**
     * Get the first (singleton/default) clinic details record.
     */
    Optional<ClinicDetails> findFirstByIsDeletedFalseOrderByIdAsc();

    /**
     * Find active clinic details by ID.
     */
    Optional<ClinicDetails> findByIdAndIsDeletedFalse(Long id);

    /**
     * Find active clinic details by clinicId for multi-tenancy.
     */
    Optional<ClinicDetails> findByClinicIdAndIsDeletedFalse(Long clinicId);

    /**
     * Check if any active clinic details record exists.
     */
    boolean existsByIsDeletedFalse();

    /**
     * Check if active clinic details exist for a specific clinicId.
     */
    boolean existsByClinicIdAndIsDeletedFalse(Long clinicId);

    /**
     * Find active clinic details by API key for clinic-wide external integrations (e.g. Retell AI agent).
     */
    Optional<ClinicDetails> findByApiKeyAndIsDeletedFalse(String apiKey);

    /**
     * Check if active clinic details exist with the given API key.
     */
    boolean existsByApiKeyAndIsDeletedFalse(String apiKey);

    /**
     * Find all non-deleted clinics.
     */
    List<ClinicDetails> findByIsDeletedFalse();
}
