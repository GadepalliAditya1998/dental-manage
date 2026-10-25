package com.dental.manage.service;

import com.dental.manage.dto.response.DashboardMetricsResponse;

import java.time.LocalDate;

/**
 * Service for calculating and aggregating dashboard metrics for receptionist and doctor portals.
 */
public interface DashboardService {

    /**
     * Retrieve aggregated dashboard metrics based on time filter (DAY, WEEK, MONTH, CUSTOM)
     * and optional practitioner (doctorId) and tenant (clinicId) scopes.
     *
     * @param clinicId Tenant clinic ID
     * @param doctorId Optional doctor ID for doctor portal scoping
     * @param filter Time range filter: DAY, WEEK, MONTH, CUSTOM
     * @param startDate Optional custom start date (used if filter is CUSTOM)
     * @param endDate Optional custom end date (used if filter is CUSTOM)
     * @return DashboardMetricsResponse containing call and appointment metrics
     */
    DashboardMetricsResponse getMetrics(Long clinicId, Long doctorId, String filter, LocalDate startDate, LocalDate endDate);
}
