package com.dental.manage.repository;

import com.dental.manage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndIsDeletedFalse(String username);

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByUsernameAndIsDeletedFalse(String username);

    boolean existsByPassword(String password);

    Optional<User> findByPatientIdAndIsDeletedFalse(String patientId);

    /**
     * Looks up patients matching string patient ID.
     * Excludes non-patient roles (e.g. DOCTOR, RECEPTIONIST).
     *
     * @param patientId string patient identifier
     * @param clinicId optional clinicId filter
     * @return List of matching patient users
     */
    @Query("SELECT u FROM User u WHERE u.role = com.dental.manage.enums.UserRole.PATIENT " +
           "AND u.isDeleted = false " +
           "AND (LOWER(TRIM(u.patientId)) = LOWER(TRIM(:patientId)) OR " +
           "     CAST(u.id AS string) = TRIM(:patientId)) " +
           "AND (:clinicId IS NULL OR u.clinicId = :clinicId)")
    List<User> findPatientsByPatientId(
            @Param("patientId") String patientId,
            @Param("clinicId") Long clinicId);

    /**
     * Looks up patients matching phone number and date of birth.
     * Excludes non-patient roles (e.g. DOCTOR, RECEPTIONIST).
     *
     * @param phoneNumber patient phone number
     * @param dateOfBirth patient date of birth
     * @param clinicId optional clinicId filter
     * @return List of matching patient users
     */
    @Query("SELECT u FROM User u WHERE u.role = com.dental.manage.enums.UserRole.PATIENT " +
           "AND u.isDeleted = false " +
           "AND (TRIM(u.phoneNumber) = TRIM(:phoneNumber) OR " +
           "     REPLACE(REPLACE(REPLACE(TRIM(u.phoneNumber), '-', ''), ' ', ''), '+', '') = " +
           "     REPLACE(REPLACE(REPLACE(TRIM(:phoneNumber), '-', ''), ' ', ''), '+', '')) " +
           "AND u.dateOfBirth = :dateOfBirth " +
           "AND (:clinicId IS NULL OR u.clinicId = :clinicId)")
    List<User> findPatientsByPhoneAndDob(
            @Param("phoneNumber") String phoneNumber,
            @Param("dateOfBirth") LocalDate dateOfBirth,
            @Param("clinicId") Long clinicId);

    /**
     * Looks up patients matching phone number, full name, and date of birth.
     * Excludes non-patient roles (e.g. DOCTOR, RECEPTIONIST).
     *
     * @param phoneNumber patient phone number
     * @param fullName patient full name (case-insensitive)
     * @param dateOfBirth patient date of birth
     * @param clinicId optional clinicId filter
     * @return List of matching patient users
     */
    @Query("SELECT u FROM User u WHERE u.role = com.dental.manage.enums.UserRole.PATIENT " +
           "AND u.isDeleted = false " +
           "AND (TRIM(u.phoneNumber) = TRIM(:phoneNumber) OR " +
           "     REPLACE(REPLACE(REPLACE(TRIM(u.phoneNumber), '-', ''), ' ', ''), '+', '') = " +
           "     REPLACE(REPLACE(REPLACE(TRIM(:phoneNumber), '-', ''), ' ', ''), '+', '')) " +
           "AND LOWER(TRIM(u.fullName)) = LOWER(TRIM(:fullName)) " +
           "AND u.dateOfBirth = :dateOfBirth " +
           "AND (:clinicId IS NULL OR u.clinicId = :clinicId)")
    List<User> findPatientsForLookup(
            @Param("phoneNumber") String phoneNumber,
            @Param("fullName") String fullName,
            @Param("dateOfBirth") LocalDate dateOfBirth,
            @Param("clinicId") Long clinicId);

    // Doctor role queries
    @Query("SELECT u FROM User u WHERE u.role = com.dental.manage.enums.UserRole.DOCTOR " +
           "AND u.isDeleted = false " +
           "AND (:clinicId IS NULL OR u.clinicId = :clinicId) " +
           "AND (:activeOnly = false OR u.isActive = true) " +
           "ORDER BY u.id ASC")
    List<User> findDoctors(
            @Param("clinicId") Long clinicId,
            @Param("activeOnly") boolean activeOnly);

    Optional<User> findByIdAndRoleAndIsDeletedFalse(Long id, com.dental.manage.enums.UserRole role);

    Optional<User> findByIdAndClinicIdAndRoleAndIsDeletedFalse(Long id, Long clinicId, com.dental.manage.enums.UserRole role);

    List<User> findByRoleAndIsDeletedFalse(com.dental.manage.enums.UserRole role);

    List<User> findByClinicIdAndRoleAndIsDeletedFalse(Long clinicId, com.dental.manage.enums.UserRole role);

    List<User> findByClinicIdAndRoleAndIsActiveTrueAndIsDeletedFalse(Long clinicId, com.dental.manage.enums.UserRole role);

    long countByClinicIdAndRoleAndIsActiveTrueAndIsDeletedFalse(Long clinicId, com.dental.manage.enums.UserRole role);
}
