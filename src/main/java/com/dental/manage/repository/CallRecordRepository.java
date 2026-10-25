package com.dental.manage.repository;

import com.dental.manage.entity.CallRecord;
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
public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {

    Optional<CallRecord> findByRetellCallId(String retellCallId);

    Optional<CallRecord> findByIdAndIsDeletedFalse(Long id);

    Page<CallRecord> findByIsDeletedFalse(Pageable pageable);

    @Query("SELECT cr FROM CallRecord cr WHERE cr.isDeleted = false " +
           "AND cr.callStartedAt BETWEEN :start AND :end")
    List<CallRecord> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(cr) FROM CallRecord cr WHERE cr.isDeleted = false " +
           "AND cr.callType = com.dental.manage.enums.CallType.AI_INBOUND AND cr.callStartedAt BETWEEN :start AND :end")
    long countAiCallsByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(cr) FROM CallRecord cr WHERE cr.isDeleted = false " +
           "AND cr.transferredToHuman = true AND cr.callStartedAt BETWEEN :start AND :end")
    long countHumanTransfersByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT AVG(cr.durationMs) FROM CallRecord cr WHERE cr.isDeleted = false " +
           "AND cr.callStartedAt BETWEEN :start AND :end AND cr.durationMs IS NOT NULL")
    Double avgDurationByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Multi-tenant queries
    List<CallRecord> findByClinicIdAndIsDeletedFalse(Long clinicId);

    Page<CallRecord> findByClinicIdAndIsDeletedFalse(Long clinicId, Pageable pageable);

    Optional<CallRecord> findByIdAndClinicIdAndIsDeletedFalse(Long id, Long clinicId);

    Optional<CallRecord> findByClinicIdAndRetellCallId(Long clinicId, String retellCallId);

    @Query("SELECT cr FROM CallRecord cr WHERE cr.clinicId = :clinicId AND cr.isDeleted = false " +
           "AND cr.callStartedAt BETWEEN :start AND :end")
    List<CallRecord> findByClinicIdAndDateRange(
            @Param("clinicId") Long clinicId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Filter and Metric Queries
    @Query("SELECT COUNT(cr) FROM CallRecord cr WHERE cr.isDeleted = false " +
           "AND (:clinicId IS NULL OR cr.clinicId = :clinicId) " +
           "AND ((cr.callStartedAt IS NOT NULL AND cr.callStartedAt BETWEEN :start AND :end) " +
           "  OR (cr.callStartedAt IS NULL AND cr.createdOn BETWEEN :start AND :end))")
    long countCallsByClinicAndDateRange(
            @Param("clinicId") Long clinicId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT a.callRecord.id) FROM Appointment a " +
           "WHERE a.isDeleted = false AND a.doctor.id = :doctorId " +
           "AND a.callRecord IS NOT NULL " +
           "AND (:clinicId IS NULL OR a.clinicId = :clinicId) " +
           "AND ((a.callRecord.callStartedAt IS NOT NULL AND a.callRecord.callStartedAt BETWEEN :start AND :end) " +
           "  OR (a.callRecord.callStartedAt IS NULL AND a.callRecord.createdOn BETWEEN :start AND :end) " +
           "  OR (a.createdOn BETWEEN :start AND :end))")
    long countCallsByDoctorAndDateRange(
            @Param("clinicId") Long clinicId,
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(cr) FROM CallRecord cr WHERE cr.isDeleted = false " +
           "AND (:clinicId IS NULL OR cr.clinicId = :clinicId) " +
           "AND (cr.callType = com.dental.manage.enums.CallType.AI_INBOUND) " +
           "AND ((cr.callStartedAt IS NOT NULL AND cr.callStartedAt BETWEEN :start AND :end) " +
           "  OR (cr.callStartedAt IS NULL AND cr.createdOn BETWEEN :start AND :end))")
    long countAiCallsByClinicAndDateRange(
            @Param("clinicId") Long clinicId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(cr) FROM CallRecord cr WHERE cr.isDeleted = false " +
           "AND (:clinicId IS NULL OR cr.clinicId = :clinicId) " +
           "AND cr.transferredToHuman = true " +
           "AND ((cr.callStartedAt IS NOT NULL AND cr.callStartedAt BETWEEN :start AND :end) " +
           "  OR (cr.callStartedAt IS NULL AND cr.createdOn BETWEEN :start AND :end))")
    long countHumanTransfersByClinicAndDateRange(
            @Param("clinicId") Long clinicId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT AVG(cr.durationMs) FROM CallRecord cr WHERE cr.isDeleted = false " +
           "AND (:clinicId IS NULL OR cr.clinicId = :clinicId) " +
           "AND cr.durationMs IS NOT NULL " +
           "AND ((cr.callStartedAt IS NOT NULL AND cr.callStartedAt BETWEEN :start AND :end) " +
           "  OR (cr.callStartedAt IS NULL AND cr.createdOn BETWEEN :start AND :end))")
    Double avgDurationByClinicAndDateRange(
            @Param("clinicId") Long clinicId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT AVG(cr.sentimentScore) FROM CallRecord cr WHERE cr.isDeleted = false " +
           "AND (:clinicId IS NULL OR cr.clinicId = :clinicId) " +
           "AND cr.sentimentScore IS NOT NULL " +
           "AND ((cr.callStartedAt IS NOT NULL AND cr.callStartedAt BETWEEN :start AND :end) " +
           "  OR (cr.callStartedAt IS NULL AND cr.createdOn BETWEEN :start AND :end))")
    Double avgSentimentByClinicAndDateRange(
            @Param("clinicId") Long clinicId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT cr FROM CallRecord cr WHERE cr.isDeleted = false " +
           "AND (:clinicId IS NULL OR cr.clinicId = :clinicId) " +
           "AND ((cr.callStartedAt IS NOT NULL AND cr.callStartedAt BETWEEN :start AND :end) " +
           "  OR (cr.callStartedAt IS NULL AND cr.createdOn BETWEEN :start AND :end)) " +
           "ORDER BY COALESCE(cr.callStartedAt, cr.createdOn) ASC")
    List<CallRecord> findCallsByClinicAndDateRange(
            @Param("clinicId") Long clinicId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT DISTINCT a.callRecord FROM Appointment a " +
           "WHERE a.isDeleted = false AND a.doctor.id = :doctorId " +
           "AND a.callRecord IS NOT NULL " +
           "AND (:clinicId IS NULL OR a.clinicId = :clinicId) " +
           "AND ((a.callRecord.callStartedAt IS NOT NULL AND a.callRecord.callStartedAt BETWEEN :start AND :end) " +
           "  OR (a.callRecord.callStartedAt IS NULL AND a.callRecord.createdOn BETWEEN :start AND :end) " +
           "  OR (a.createdOn BETWEEN :start AND :end)) " +
           "ORDER BY COALESCE(a.callRecord.callStartedAt, a.callRecord.createdOn) ASC")
    List<CallRecord> findCallsByDoctorAndDateRange(
            @Param("clinicId") Long clinicId,
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT cr FROM CallRecord cr WHERE cr.isDeleted = false " +
           "AND (:clinicId IS NULL OR cr.clinicId = :clinicId) " +
           "ORDER BY COALESCE(cr.callStartedAt, cr.createdOn) DESC")
    List<CallRecord> findRecentCallsByClinic(
            @Param("clinicId") Long clinicId,
            Pageable pageable);

    @Query("SELECT DISTINCT a.callRecord FROM Appointment a " +
           "WHERE a.isDeleted = false AND a.doctor.id = :doctorId " +
           "AND a.callRecord IS NOT NULL " +
           "AND (:clinicId IS NULL OR a.clinicId = :clinicId) " +
           "ORDER BY COALESCE(a.callRecord.callStartedAt, a.callRecord.createdOn) DESC")
    List<CallRecord> findRecentCallsByDoctor(
            @Param("clinicId") Long clinicId,
            @Param("doctorId") Long doctorId,
            Pageable pageable);
}
