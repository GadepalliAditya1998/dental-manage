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
}
