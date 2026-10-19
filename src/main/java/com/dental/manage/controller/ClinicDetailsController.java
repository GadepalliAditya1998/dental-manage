package com.dental.manage.controller;

import com.dental.manage.dto.request.ClinicSettingsRequest;
import com.dental.manage.dto.response.ApiResponse;
import com.dental.manage.dto.response.ClinicSettingsResponse;
import com.dental.manage.dto.response.ClinicWorkingHoursResponse;
import com.dental.manage.service.ClinicDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * REST Controller for retrieving clinic details and day-wise working hours.
 * Supports querying working hours for a day or for a week based on request parameters.
 */
@RestController
@RequestMapping({"/api/clinic", "/api/clinic/details"})
@RequiredArgsConstructor
@Slf4j
public class ClinicDetailsController {

    private final ClinicDetailsService clinicDetailsService;

    /**
     * Retrieves clinic details along with day-wise working hours.
     *
     * @param period "day" or "week" (case-insensitive, defaults to "week")
     * @param view alias for period ("day" or "week")
     * @param dateTime optional target datetime
     * @param from alias for dateTime (optional target datetime)
     * @param dayOfWeek optional day of week name (e.g., "MONDAY")
     * @return ApiResponse containing ClinicWorkingHoursResponse with day-wise records
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ClinicWorkingHoursResponse>> getClinicDetails(
            @RequestParam(name = "period", required = false) String period,
            @RequestParam(name = "view", required = false) String view,
            @RequestParam(name = "dateTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(name = "dayOfWeek", required = false) String dayOfWeek) {
        String effectivePeriod = (period != null && !period.isBlank())
                ? period
                : ((view != null && !view.isBlank()) ? view : "WEEK");
        LocalDateTime effectiveDateTime = dateTime != null ? dateTime : from;

        log.info("Request received to fetch clinic details (period={}, dateTime={}, dayOfWeek={})",
                effectivePeriod, effectiveDateTime, dayOfWeek);

        ClinicWorkingHoursResponse response = clinicDetailsService.getClinicDetailsWithWorkingHours(
                effectivePeriod, effectiveDateTime, dayOfWeek);
        return ResponseEntity.ok(ApiResponse.success("Clinic details retrieved successfully", response));
    }

    /**
     * Dedicated endpoint to retrieve clinic details and working hours for a single day.
     *
     * @param dateTime optional target datetime
     * @param from alias for dateTime
     * @return ApiResponse containing ClinicWorkingHoursResponse with 1 day record
     */
    @GetMapping("/day")
    public ResponseEntity<ApiResponse<ClinicWorkingHoursResponse>> getClinicDetailsForDay(
            @RequestParam(name = "dateTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from) {
        LocalDateTime effectiveDateTime = dateTime != null ? dateTime : from;
        log.info("Request received to fetch clinic details for day (dateTime={})", effectiveDateTime);
        ClinicWorkingHoursResponse response = clinicDetailsService.getClinicDetailsForDay(effectiveDateTime);
        return ResponseEntity.ok(ApiResponse.success("Clinic details for day retrieved successfully", response));
    }

    /**
     * Dedicated endpoint to retrieve clinic details and working hours for a full week.
     *
     * @param dateTime optional target datetime
     * @param from alias for dateTime
     * @return ApiResponse containing ClinicWorkingHoursResponse with 7 day-wise records
     */
    @GetMapping("/week")
    public ResponseEntity<ApiResponse<ClinicWorkingHoursResponse>> getClinicDetailsForWeek(
            @RequestParam(name = "dateTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from) {
        LocalDateTime effectiveDateTime = dateTime != null ? dateTime : from;
        log.info("Request received to fetch clinic details for week (dateTime={})", effectiveDateTime);
        ClinicWorkingHoursResponse response = clinicDetailsService.getClinicDetailsForWeek(effectiveDateTime);
        return ResponseEntity.ok(ApiResponse.success("Clinic details for week retrieved successfully", response));
    }

    /**
     * Fetches raw clinic configuration settings.
     *
     * @return ApiResponse containing ClinicSettingsResponse
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<ClinicSettingsResponse>> getClinicSettings() {
        log.info("Request received to fetch clinic configuration settings");
        ClinicSettingsResponse response = clinicDetailsService.getClinicSettings();
        return ResponseEntity.ok(ApiResponse.success("Clinic settings retrieved successfully", response));
    }

    /**
     * Updates the clinic configuration settings.
     *
     * @param request Update payload
     * @return ApiResponse containing updated ClinicSettingsResponse
     */
    @PutMapping
    public ResponseEntity<ApiResponse<ClinicSettingsResponse>> updateClinicDetails(
            @Valid @RequestBody ClinicSettingsRequest request) {
        log.info("Request received to update clinic configuration");
        ClinicSettingsResponse response = clinicDetailsService.updateClinicDetails(request);
        return ResponseEntity.ok(ApiResponse.success("Clinic details updated successfully", response));
    }
}
