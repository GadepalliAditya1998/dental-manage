package com.dental.manage.repository;

import com.dental.manage.entity.Appointment;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.BookingSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByIdAndIsDeletedFalse(Long id);

    Page<Appointment> findByIsDeletedFalse(Pageable pageable);

    /**
     * Find appointments for a specific doctor within a date range.
     * Used for slot availability calculation.
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.appointmentStart >= :start AND a.appointmentEnd <= :end " +
           "AND a.status NOT IN ('CANCELLED') AND a.isDeleted = false")
    List<Appointment> findByDoctorIdAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Find appointments needing reminders: status is SCHEDULED and
     * appointment starts within the given time window.
     */
    @Query("SELECT a FROM Appointment a WHERE a.status = :status " +
           "AND a.appointmentStart BETWEEN :start AND :end " +
           "AND a.reminderSentAt IS NULL AND a.isDeleted = false")
    List<Appointment> findAppointmentsNeedingReminder(
            @Param("status") AppointmentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Find appointments needing WhatsApp reminders for tomorrow with patient, doctor, and service details.
     * Excludes patients who opted out of notifications.
     */
    @Query("SELECT a FROM Appointment a " +
           "JOIN FETCH a.patient p " +
           "JOIN FETCH a.doctor d " +
           "JOIN FETCH a.service s " +
           "WHERE a.status = :status " +
           "AND a.appointmentStart BETWEEN :start AND :end " +
           "AND a.reminderSentAt IS NULL " +
           "AND (p.whatsappOptOut IS NULL OR p.whatsappOptOut = false) " +
           "AND a.isDeleted = false")
    List<Appointment> findAppointmentsNeedingReminderTomorrow(
            @Param("status") AppointmentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Find by patient phone number — used for SMS confirmation replies.
     */
    @Query("SELECT a FROM Appointment a WHERE a.patient.phoneNumber = :phone " +
           "AND a.status IN ('SCHEDULED', 'REMINDER_SENT') " +
           "AND a.appointmentStart > CURRENT_TIMESTAMP " +
           "AND a.isDeleted = false " +
           "ORDER BY a.appointmentStart ASC")
    List<Appointment> findUpcomingByPatientPhone(@Param("phone") String phone);

    Page<Appointment> findByStatusAndIsDeletedFalse(AppointmentStatus status, Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE a.isDeleted = false " +
           "AND a.appointmentStart BETWEEN :start AND :end")
    Page<Appointment> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.isDeleted = false " +
           "AND a.status = :status AND a.createdOn BETWEEN :start AND :end")
    long countByStatusAndDateRange(
            @Param("status") AppointmentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Multi-tenant queries
    List<Appointment> findByClinicIdAndIsDeletedFalse(Long clinicId);

    Page<Appointment> findByClinicIdAndIsDeletedFalse(Long clinicId, Pageable pageable);

    Optional<Appointment> findByIdAndClinicIdAndIsDeletedFalse(Long id, Long clinicId);

    @Query("SELECT a FROM Appointment a WHERE a.clinicId = :clinicId " +
           "AND a.doctor.id = :doctorId " +
           "AND a.appointmentStart >= :start AND a.appointmentEnd <= :end " +
           "AND a.status NOT IN ('CANCELLED') AND a.isDeleted = false")
    List<Appointment> findByClinicIdAndDoctorIdAndDateRange(
            @Param("clinicId") Long clinicId,
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Appointment a WHERE a.clinicId = :clinicId " +
           "AND a.isDeleted = false AND a.appointmentStart BETWEEN :start AND :end")
    Page<Appointment> findByClinicIdAndDateRange(
            @Param("clinicId") Long clinicId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    /**
     * Find overlapping appointments for a doctor between start and end times (excluding CANCELLED).
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.appointmentStart < :end AND a.appointmentEnd > :start " +
           "AND a.status NOT IN ('CANCELLED') AND a.isDeleted = false")
    List<Appointment> findOverlappingDoctorAppointments(
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Find overlapping appointments for a doctor excluding a specific appointment ID.
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.id <> :excludeId " +
           "AND a.appointmentStart < :end AND a.appointmentEnd > :start " +
           "AND a.status NOT IN ('CANCELLED') AND a.isDeleted = false")
    List<Appointment> findOverlappingDoctorAppointmentsExcluding(
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludeId") Long excludeId);

    List<Appointment> findByPatientIdAndIsDeletedFalseOrderByAppointmentStartDesc(Long patientId);

    List<Appointment> findByDoctorIdAndIsDeletedFalseOrderByAppointmentStartDesc(Long doctorId);

    /**
     * Flexible filtering for appointments with optional clinicId, doctorId, patientId, status, bookingSource, and date range.
     */
    @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.callRecord WHERE a.isDeleted = false " +
           "AND (:clinicId IS NULL OR a.clinicId = :clinicId) " +
           "AND (:doctorId IS NULL OR a.doctor.id = :doctorId) " +
           "AND (:patientId IS NULL OR a.patient.id = :patientId) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:bookingSource IS NULL OR a.bookingSource = :bookingSource) " +
           "AND (cast(:start as timestamp) IS NULL OR a.appointmentStart >= :start) " +
           "AND (cast(:end as timestamp) IS NULL OR a.appointmentStart <= :end) " +
           "ORDER BY a.appointmentStart ASC")
    List<Appointment> findFilteredAppointments(
            @Param("clinicId") Long clinicId,
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId,
            @Param("status") AppointmentStatus status,
            @Param("bookingSource") BookingSource bookingSource,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    default List<Appointment> findFilteredAppointments(
            Long clinicId,
            Long doctorId,
            Long patientId,
            AppointmentStatus status,
            LocalDateTime start,
            LocalDateTime end) {
        return findFilteredAppointments(clinicId, doctorId, patientId, status, null, start, end);
    }

    // Dashboard Metric Queries
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.isDeleted = false " +
           "AND (:clinicId IS NULL OR a.clinicId = :clinicId) " +
           "AND (:doctorId IS NULL OR a.doctor.id = :doctorId) " +
           "AND (a.bookingSource = :aiAgentSource OR a.callRecord IS NOT NULL) " +
           "AND ((a.createdOn IS NOT NULL AND a.createdOn BETWEEN :start AND :end) " +
           "  OR (a.appointmentStart BETWEEN :start AND :end))")
    long countAiAgentBookingsByDateRange(
            @Param("clinicId") Long clinicId,
            @Param("doctorId") Long doctorId,
            @Param("aiAgentSource") BookingSource aiAgentSource,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.isDeleted = false " +
           "AND (:clinicId IS NULL OR a.clinicId = :clinicId) " +
           "AND (:doctorId IS NULL OR a.doctor.id = :doctorId) " +
           "AND a.status IN (com.dental.manage.enums.AppointmentStatus.SCHEDULED, com.dental.manage.enums.AppointmentStatus.CONFIRMED, com.dental.manage.enums.AppointmentStatus.COMPLETED) " +
           "AND ((a.createdOn IS NOT NULL AND a.createdOn BETWEEN :start AND :end) " +
           "  OR (a.appointmentStart BETWEEN :start AND :end))")
    long countSuccessfulBookingsByDateRange(
            @Param("clinicId") Long clinicId,
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.isDeleted = false " +
           "AND (:clinicId IS NULL OR a.clinicId = :clinicId) " +
           "AND (:doctorId IS NULL OR a.doctor.id = :doctorId) " +
           "AND a.patient.isNewLead = true " +
           "AND ((a.createdOn IS NOT NULL AND a.createdOn BETWEEN :start AND :end) " +
           "  OR (a.appointmentStart BETWEEN :start AND :end))")
    long countNewLeadBookingsByDateRange(
            @Param("clinicId") Long clinicId,
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.isDeleted = false " +
           "AND (:clinicId IS NULL OR a.clinicId = :clinicId) " +
           "AND (:doctorId IS NULL OR a.doctor.id = :doctorId) " +
           "AND a.status = :status " +
           "AND ((a.createdOn IS NOT NULL AND a.createdOn BETWEEN :start AND :end) " +
           "  OR (a.appointmentStart BETWEEN :start AND :end))")
    long countByStatusClinicDoctorAndDateRange(
            @Param("clinicId") Long clinicId,
            @Param("doctorId") Long doctorId,
            @Param("status") AppointmentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.callRecord LEFT JOIN FETCH a.patient LEFT JOIN FETCH a.doctor LEFT JOIN FETCH a.service " +
           "WHERE a.isDeleted = false " +
           "AND (:clinicId IS NULL OR a.clinicId = :clinicId) " +
           "AND (:doctorId IS NULL OR a.doctor.id = :doctorId) " +
           "AND ((a.createdOn IS NOT NULL AND a.createdOn BETWEEN :start AND :end) " +
           "  OR (a.appointmentStart BETWEEN :start AND :end)) " +
           "ORDER BY a.appointmentStart ASC")
    List<Appointment> findAppointmentsByClinicDoctorAndDateRange(
            @Param("clinicId") Long clinicId,
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
