package com.dental.manage.repository;

import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientIdAndIsDeletedFalse(String patientId);

    Optional<Patient> findByClinicIdAndPatientIdAndIsDeletedFalse(Long clinicId, String patientId);

    Optional<Patient> findByPhoneNumberAndDateOfBirthAndIsDeletedFalse(String phoneNumber, LocalDate dateOfBirth);

    Optional<Patient> findByClinicIdAndPhoneNumberAndDateOfBirthAndIsDeletedFalse(Long clinicId, String phoneNumber, LocalDate dateOfBirth);

    Optional<Patient> findByPhoneNumberAndIsDeletedFalse(String phoneNumber);

    boolean existsByPhoneNumberAndIsDeletedFalse(String phoneNumber);

    Optional<Patient> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT p FROM Patient p WHERE p.isDeleted = false " +
           "AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR p.phoneNumber LIKE CONCAT('%', :search, '%') " +
           "OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Patient> searchPatients(@Param("search") String search, Pageable pageable);

    Page<Patient> findByIsDeletedFalse(Pageable pageable);

    // Multi-tenant queries
    List<Patient> findByClinicIdAndIsDeletedFalse(Long clinicId);

    Page<Patient> findByClinicIdAndIsDeletedFalse(Long clinicId, Pageable pageable);

    Optional<Patient> findByClinicIdAndPhoneNumberAndIsDeletedFalse(Long clinicId, String phoneNumber);

    Optional<Patient> findByIdAndClinicIdAndIsDeletedFalse(Long id, Long clinicId);

    @Query("SELECT p FROM Patient p WHERE p.clinicId = :clinicId AND p.isDeleted = false " +
           "AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR p.phoneNumber LIKE CONCAT('%', :search, '%') " +
           "OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Patient> searchPatientsByClinicId(@Param("clinicId") Long clinicId, @Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE p.isDeleted = false " +
           "AND (:clinicId IS NULL OR p.clinicId = :clinicId) " +
           "AND (:search IS NULL OR :search = '' " +
           "     OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR p.phoneNumber LIKE CONCAT('%', :search, '%') " +
           "     OR LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(p.patientId) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.id DESC")
    List<Patient> findFilteredPatients(@Param("clinicId") Long clinicId, @Param("search") String search);

    /**
     * Find upcoming appointments for a patient starting from a given datetime.
     * Excludes cancelled and completed appointments, and eagerly fetches doctor and service.
     */
    @Query("SELECT a FROM Appointment a " +
           "JOIN FETCH a.patient p " +
           "JOIN FETCH a.doctor d " +
           "JOIN FETCH a.service s " +
           "WHERE p.id = :patientId " +
           "AND a.appointmentStart >= :fromDateTime " +
           "AND a.status NOT IN ('CANCELLED', 'COMPLETED') " +
           "AND a.isDeleted = false " +
           "ORDER BY a.appointmentStart ASC")
    List<Appointment> findUpcomingAppointmentsByPatientId(
            @Param("patientId") Long patientId,
            @Param("fromDateTime") LocalDateTime fromDateTime);
}
