package com.dental.manage.service;

import com.dental.manage.dto.request.ClinicSettingsRequest;
import com.dental.manage.dto.response.ClinicSettingsResponse;
import com.dental.manage.dto.response.ClinicWorkingHoursResponse;
import com.dental.manage.entity.ClinicDetails;

import java.time.LocalDateTime;

/**
 * Service interface for managing clinic details and working hours.
 */
public interface ClinicDetailsService {

    /**
     * Retrieves clinic details along with day-wise working hours.
     *
     * @param period "day" or "week" (case-insensitive, defaults to "week" if null or empty)
     * @param dateTime optional target datetime for calculating the day or week range (defaults to now in clinic timezone)
     * @param dayOfWeek optional day of week name (e.g., "MONDAY")
     * @return ClinicWorkingHoursResponse with clinic details and day-wise working hours
     */
    ClinicWorkingHoursResponse getClinicDetailsWithWorkingHours(String period, LocalDateTime dateTime, String dayOfWeek);

    /**
     * Retrieves clinic details along with working hours for a single day.
     *
     * @param dateTime optional target datetime (defaults to now in clinic timezone)
     * @return ClinicWorkingHoursResponse with a single day working hours record
     */
    ClinicWorkingHoursResponse getClinicDetailsForDay(LocalDateTime dateTime);

    /**
     * Retrieves clinic details along with working hours for a full week (Monday through Sunday).
     *
     * @param dateTime optional target datetime (defaults to now in clinic timezone)
     * @return ClinicWorkingHoursResponse with 7 day-wise working hours records
     */
    ClinicWorkingHoursResponse getClinicDetailsForWeek(LocalDateTime dateTime);

    /**
     * Retrieves the singleton clinic configuration settings.
     *
     * @return ClinicSettingsResponse
     */
    ClinicSettingsResponse getClinicSettings();

    /**
     * Updates the singleton clinic details configuration.
     *
     * @param request Clinic details update payload
     * @return Updated ClinicSettingsResponse
     */
    ClinicSettingsResponse updateClinicDetails(ClinicSettingsRequest request);

    /**
     * Retrieves the raw ClinicDetails entity.
     *
     * @return ClinicDetails entity
     */
    ClinicDetails getClinicDetailsEntity();
}
