package com.dental.manage.repository;

import com.dental.manage.entity.CallMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CallMetricRepository extends JpaRepository<CallMetric, Long> {

    Optional<CallMetric> findByMetricDate(LocalDate metricDate);

    @Query("SELECT cm FROM CallMetric cm WHERE cm.metricDate BETWEEN :from AND :to " +
           "AND cm.isDeleted = false ORDER BY cm.metricDate ASC")
    List<CallMetric> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
