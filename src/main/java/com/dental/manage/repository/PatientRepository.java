package com.dental.manage.repository;

import com.dental.manage.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPhoneNumberAndIsDeletedFalse(String phoneNumber);

    Optional<Patient> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT p FROM Patient p WHERE p.isDeleted = false " +
           "AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR p.phoneNumber LIKE CONCAT('%', :search, '%') " +
           "OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Patient> searchPatients(@Param("search") String search, Pageable pageable);

    Page<Patient> findByIsDeletedFalse(Pageable pageable);
}
