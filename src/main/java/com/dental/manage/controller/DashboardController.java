package com.dental.manage.controller;

import com.dental.manage.context.ClinicRequestContext;
import com.dental.manage.dto.response.ApiResponse;
import com.dental.manage.dto.response.DashboardMetricsResponse;
import com.dental.manage.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controller for providing dashboard metrics to receptionists and doctors.
 * Supports filtering by DAY, WEEK, MONTH, and CUSTOM date ranges.
 */
@RestController
@RequestMapping({"/api/dashboard", "/api/clinics/{clinicId}/dashboard"})
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Retrieve aggregated metrics for the dashboard.
     *
     * @param filter Time range filter: DAY, WEEK, MONTH, CUSTOM
     * @param startDate Custom start date (for CUSTOM filter)
     * @param endDate Custom end date (for CUSTOM filter)
     * @param doctorId Optional doctorId for practitioner-scoped metrics
     * @param clinicId Optional clinicId query param
     * @return ApiResponse containing DashboardMetricsResponse
     */
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getMetrics(
            @RequestParam(name = "filter", defaultValue = "DAY") String filter,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "clinicId", required = false) Long clinicId) {

        Long effectiveClinicId = clinicId != null ? clinicId : ClinicRequestContext.getClinicId();
        log.info("Request received for dashboard metrics: filter={}, startDate={}, endDate={}, doctorId={}, clinicId={}",
                filter, startDate, endDate, doctorId, effectiveClinicId);

        DashboardMetricsResponse response = dashboardService.getMetrics(
                effectiveClinicId, doctorId, filter, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success("Dashboard metrics retrieved successfully", response));
    }

    /**
     * Retrieve daily metrics breakdown.
     *
     * @param startDate Optional start date
     * @param endDate Optional end date
     * @param doctorId Optional doctor ID
     * @param clinicId Optional clinic ID
     * @return ApiResponse containing DashboardMetricsResponse with daily metrics
     */
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getDailyMetrics(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "clinicId", required = false) Long clinicId) {

        Long effectiveClinicId = clinicId != null ? clinicId : ClinicRequestContext.getClinicId();
        String filter = (startDate != null || endDate != null) ? "CUSTOM" : "WEEK";

        DashboardMetricsResponse response = dashboardService.getMetrics(
                effectiveClinicId, doctorId, filter, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success("Daily metrics retrieved successfully", response));
    }
}
