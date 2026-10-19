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
           "AND cr.callType = 'AI_INBOUND' AND cr.callStartedAt BETWEEN :start AND :end")
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
}
