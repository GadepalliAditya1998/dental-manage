package com.dental.manage.repository;

import com.dental.manage.entity.Appointment;
import com.dental.manage.enums.AppointmentStatus;
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
}
