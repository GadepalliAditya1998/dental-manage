package com.dental.manage.repository;

import com.dental.manage.entity.ClinicDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClinicDetailsRepository extends JpaRepository<ClinicDetails, Long> {

    /**
     * Get the first (singleton) clinic details record.
     */
    Optional<ClinicDetails> findFirstByIsDeletedFalseOrderByIdAsc();
}
