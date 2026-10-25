package com.dental.manage.service.impl;

import com.dental.manage.context.ClinicRequestContext;
import com.dental.manage.dto.request.ClinicSettingsRequest;
import com.dental.manage.dto.response.ApiKeyResponse;
import com.dental.manage.dto.response.ClinicSettingsResponse;
import com.dental.manage.dto.response.ClinicWorkingHoursResponse;
import com.dental.manage.entity.ClinicDetails;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.mapper.ClinicDetailsMapper;
import com.dental.manage.repository.ClinicDetailsRepository;
import com.dental.manage.service.ApiKeyService;
import com.dental.manage.service.ClinicDetailsService;
import com.dental.manage.util.ApiKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Implementation of ClinicDetailsService providing clinic information and day-wise working hours.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClinicDetailsServiceImpl implements ClinicDetailsService {

    private static final String DEFAULT_TIMEZONE = "Asia/Kolkata";
    private static final String DEFAULT_WORKING_DAYS = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private final ClinicDetailsRepository clinicDetailsRepository;
    private final ClinicDetailsMapper clinicDetailsMapper;
    private final ApiKeyService apiKeyService;

    @Override
    public ClinicWorkingHoursResponse getClinicDetailsWithWorkingHours(String period, LocalDateTime dateTime, String dayOfWeek) {
        log.info("Fetching clinic details with working hours (period={}, dateTime={}, dayOfWeek={})", period, dateTime, dayOfWeek);
        ClinicDetails clinic = resolveCurrentClinicDetails(null);
        ClinicWorkingHoursResponse response = clinicDetailsMapper.toWorkingHoursResponse(clinic);

        ZoneId zoneId = resolveZoneId(clinic);
        LocalDate baseDate = (dateTime != null) ? dateTime.toLocalDate() : LocalDate.now(zoneId);
        Set<DayOfWeek> workingDays = parseWorkingDays(clinic.getWorkingDays());

        String normalizedPeriod = normalizePeriod(period);

        if ("DAY".equals(normalizedPeriod)) {
            LocalDate targetDate = resolveTargetDayDate(baseDate, dayOfWeek);
            ClinicWorkingHoursResponse.DayWorkingHours dayRecord = buildDayRecord(targetDate, workingDays, clinic);
            response.setPeriod("DAY");
            response.setWorkingHours(List.of(dayRecord));
        } else {
            LocalDate startOfWeek = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            List<ClinicWorkingHoursResponse.DayWorkingHours> weekRecords = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                LocalDate currentDay = startOfWeek.plusDays(i);
                weekRecords.add(buildDayRecord(currentDay, workingDays, clinic));
            }
            response.setPeriod("WEEK");
            response.setWorkingHours(weekRecords);
        }

        return response;
    }

    @Override
    public ClinicWorkingHoursResponse getClinicDetailsForDay(LocalDateTime dateTime) {
        return getClinicDetailsWithWorkingHours("DAY", dateTime, null);
    }

    @Override
    public ClinicWorkingHoursResponse getClinicDetailsForWeek(LocalDateTime dateTime) {
        return getClinicDetailsWithWorkingHours("WEEK", dateTime, null);
    }

    @Override
    public ClinicSettingsResponse getClinicSettings() {
        log.info("Fetching clinic configuration settings");
        ClinicDetails clinic = resolveCurrentClinicDetails(null);
        return clinicDetailsMapper.toSettingsResponse(clinic);
    }

    @Override
    @Transactional
    public ClinicSettingsResponse updateClinicDetails(ClinicSettingsRequest request) {
        log.info("Updating clinic details configuration: {}", request.getClinicName());
        ClinicDetails clinic = resolveCurrentClinicDetails(request.getClinicId());
        String existingKey = clinic.getApiKey();
        clinicDetailsMapper.updateEntityFromRequest(request, clinic);
        // Preserve existing API key if not explicitly changed in request
        if ((clinic.getApiKey() == null || clinic.getApiKey().isBlank()) && existingKey != null) {
            clinic.setApiKey(existingKey);
        } else if (clinic.getApiKey() == null || clinic.getApiKey().isBlank()) {
            clinic.setApiKey(ApiKeyGenerator.generateApiKey());
        }
        ClinicDetails saved = clinicDetailsRepository.save(clinic);
        log.info("Clinic details updated successfully (id={})", saved.getId());
        return clinicDetailsMapper.toSettingsResponse(saved);
    }

    @Override
    public ClinicDetails getClinicDetailsEntity() {
        return resolveCurrentClinicDetails(null);
    }

    @Override
    @Transactional
    public ApiKeyResponse regenerateApiKey(Long clinicId) {
        log.info("Request to regenerate Retell API key for clinicId={}", clinicId);
        return apiKeyService.createOrRegenerateRetellApiKey(clinicId, null);
    }

    @Override
    @Transactional
    public ApiKeyResponse getApiKey(Long clinicId) {
        log.info("Request to get active Retell API key for clinicId={}", clinicId);
        return apiKeyService.getActiveRetellApiKey(clinicId);
    }

    private ClinicDetails resolveCurrentClinicDetails(Long clinicId) {
        if (clinicId != null) {
            return clinicDetailsRepository.findByClinicIdAndIsDeletedFalse(clinicId)
                    .orElseGet(this::getSingletonClinicDetails);
        }
        Long contextClinicId = ClinicRequestContext.getClinicId();
        if (contextClinicId != null) {
            return clinicDetailsRepository.findByClinicIdAndIsDeletedFalse(contextClinicId)
                    .orElseGet(this::getSingletonClinicDetails);
        }
        return getSingletonClinicDetails();
    }

    private ClinicDetails getSingletonClinicDetails() {
        return clinicDetailsRepository.findFirstByIsDeletedFalseOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Clinic details configuration not found"));
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return "WEEK";
        }
        String upper = period.trim().toUpperCase(Locale.ROOT);
        if ("DAY".equals(upper) || "DAILY".equals(upper)) {
            return "DAY";
        }
        if ("WEEK".equals(upper) || "WEEKLY".equals(upper)) {
            return "WEEK";
        }
        throw new IllegalArgumentException("Invalid period '" + period + "'. Allowed values are 'day' or 'week'.");
    }

    private LocalDate resolveTargetDayDate(LocalDate baseDate, String dayOfWeek) {
        if (dayOfWeek == null || dayOfWeek.isBlank()) {
            return baseDate;
        }
        try {
            DayOfWeek requestedDow = DayOfWeek.valueOf(dayOfWeek.trim().toUpperCase(Locale.ROOT));
            LocalDate startOfWeek = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return startOfWeek.plusDays(requestedDow.getValue() - 1);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid dayOfWeek '{}' provided, defaulting to base date", dayOfWeek);
            throw new IllegalArgumentException("Invalid dayOfWeek '" + dayOfWeek + "'. Please specify a valid day (e.g. MONDAY, TUESDAY).");
        }
    }

    private ClinicWorkingHoursResponse.DayWorkingHours buildDayRecord(
            LocalDate date,
            Set<DayOfWeek> workingDays,
            ClinicDetails clinic) {
        DayOfWeek dow = date.getDayOfWeek();
        boolean isOpen = workingDays.contains(dow)
                && clinic.getOpeningTime() != null
                && clinic.getClosingTime() != null;

        LocalTime openTime = isOpen ? clinic.getOpeningTime() : null;
        LocalTime closeTime = isOpen ? clinic.getClosingTime() : null;

        return ClinicWorkingHoursResponse.DayWorkingHours.builder()
                .dayOfWeek(dow.name())
                .dateTime(date.atStartOfDay())
                .openingTime(openTime)
                .closingTime(closeTime)
                .isOpen(isOpen)
                .status(isOpen ? "OPEN" : "CLOSED")
                .formattedHours(isOpen ? formatHours(openTime, closeTime) : "Closed")
                .build();
    }

    private ZoneId resolveZoneId(ClinicDetails clinic) {
        if (clinic.getTimezone() != null && !clinic.getTimezone().isBlank()) {
            try {
                return ZoneId.of(clinic.getTimezone().trim());
            } catch (Exception e) {
                log.warn("Invalid timezone '{}' configured on clinic, falling back to {}", clinic.getTimezone(), DEFAULT_TIMEZONE);
            }
        }
        return ZoneId.of(DEFAULT_TIMEZONE);
    }

    private Set<DayOfWeek> parseWorkingDays(String workingDaysStr) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        String source = (workingDaysStr != null && !workingDaysStr.isBlank())
                ? workingDaysStr
                : DEFAULT_WORKING_DAYS;

        for (String part : source.split(",")) {
            String trimmed = part.trim().toUpperCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                try {
                    days.add(DayOfWeek.valueOf(trimmed));
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown day of week encountered in working days: {}", trimmed);
                }
            }
        }
        return days;
    }

    private String formatHours(LocalTime opening, LocalTime closing) {
        if (opening == null || closing == null) {
            return "Closed";
        }
        return (opening.format(TIME_FORMATTER) + " - " + closing.format(TIME_FORMATTER)).toUpperCase(Locale.ROOT);
    }
}
