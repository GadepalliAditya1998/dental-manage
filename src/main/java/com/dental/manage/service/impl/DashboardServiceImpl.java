package com.dental.manage.service.impl;

import com.dental.manage.dto.response.DashboardMetricsResponse;
import com.dental.manage.dto.response.DashboardMetricsResponse.DailyMetric;
import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.CallRecord;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.BookingSource;
import com.dental.manage.enums.CallType;
import com.dental.manage.repository.AppointmentRepository;
import com.dental.manage.repository.CallRecordRepository;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.enums.UserRole;
import com.dental.manage.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Implementation of DashboardService.
 * Calculates real-time metrics for calls received, AI agent bookings,
 * and daily performance breakdowns supporting Day, Week, Month, and Custom filters.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final CallRecordRepository callRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    @Override
    public DashboardMetricsResponse getMetrics(Long clinicId, Long doctorId, String filter, LocalDate startDate, LocalDate endDate) {
        String normalizedFilter = normalizeFilter(filter);
        DateRange dateRange = resolveDateRange(normalizedFilter, startDate, endDate);

        LocalDateTime start = dateRange.startDateTime();
        LocalDateTime end = dateRange.endDateTime();

        log.info("Calculating metrics for clinicId={}, doctorId={}, filter={}, start={}, end={}",
                clinicId, doctorId, normalizedFilter, start, end);

        long clinicTotalCalls = callRecordRepository.countCallsByClinicAndDateRange(clinicId, start, end);

        long callsReceived;
        if (doctorId != null) {
            long doctorCalls = callRecordRepository.countCallsByDoctorAndDateRange(clinicId, doctorId, start, end);
            if (doctorCalls > 0) {
                callsReceived = doctorCalls;
            } else {
                long activeDoctors = userRepository.countByClinicIdAndRoleAndIsActiveTrueAndIsDeletedFalse(clinicId, UserRole.DOCTOR);
                // In sole-practitioner clinics, all clinic calls belong to the doctor's agent
                callsReceived = activeDoctors <= 1 ? clinicTotalCalls : doctorCalls;
            }
        } else {
            callsReceived = clinicTotalCalls;
        }

        long appointmentsBookedViaCallAgent = appointmentRepository.countAiAgentBookingsByDateRange(
                clinicId, doctorId, BookingSource.AI_AGENT, start, end);

        long totalAiCalls = callRecordRepository.countAiCallsByClinicAndDateRange(clinicId, start, end);
        long totalHumanTransfers = callRecordRepository.countHumanTransfersByClinicAndDateRange(clinicId, start, end);
        long successfulBookings = appointmentRepository.countSuccessfulBookingsByDateRange(clinicId, doctorId, start, end);
        long newLeadBookings = appointmentRepository.countNewLeadBookingsByDateRange(clinicId, doctorId, start, end);

        long cancelledAppointments = appointmentRepository.countByStatusClinicDoctorAndDateRange(
                clinicId, doctorId, AppointmentStatus.CANCELLED, start, end);
        long rescheduledAppointments = appointmentRepository.countByStatusClinicDoctorAndDateRange(
                clinicId, doctorId, AppointmentStatus.RESCHEDULED, start, end);

        Double avgMs = callRecordRepository.avgDurationByClinicAndDateRange(clinicId, start, end);
        BigDecimal avgCallDurationSec = avgMs != null
                ? BigDecimal.valueOf(avgMs / 1000.0).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        Double avgSent = callRecordRepository.avgSentimentByClinicAndDateRange(clinicId, start, end);
        BigDecimal avgSentimentScore = avgSent != null
                ? BigDecimal.valueOf(avgSent).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        List<DailyMetric> dailyMetrics = generateDailyMetrics(clinicId, doctorId, dateRange);

        return DashboardMetricsResponse.builder()
                .clinicId(clinicId)
                .doctorId(doctorId)
                .filter(normalizedFilter)
                .startDate(dateRange.startDate().toString())
                .endDate(dateRange.endDate().toString())
                .callsReceived(callsReceived)
                .appointmentsBookedViaCallAgent(appointmentsBookedViaCallAgent)
                .clinicTotalCalls(clinicTotalCalls)
                .totalAiCalls(totalAiCalls)
                .totalHumanTransfers(totalHumanTransfers)
                .successfulBookings(successfulBookings)
                .newLeadBookings(newLeadBookings)
                .cancelledAppointments(cancelledAppointments)
                .rescheduledAppointments(rescheduledAppointments)
                .avgCallDurationSec(avgCallDurationSec)
                .avgSentimentScore(avgSentimentScore)
                .dailyMetrics(dailyMetrics)
                .build();
    }

    private List<DailyMetric> generateDailyMetrics(Long clinicId, Long doctorId, DateRange range) {
        LocalDate start = range.startDate();
        LocalDate end = range.endDate();
        long daysBetween = ChronoUnit.DAYS.between(start, end);

        // Cap daily breakdown generation to 90 days to avoid large iterations
        if (daysBetween > 90) {
            start = end.minusDays(90);
        }

        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.atTime(LocalTime.MAX);

        List<CallRecord> calls = (doctorId != null)
                ? callRecordRepository.findCallsByDoctorAndDateRange(clinicId, doctorId, rangeStart, rangeEnd)
                : callRecordRepository.findCallsByClinicAndDateRange(clinicId, rangeStart, rangeEnd);

        if (doctorId != null && calls.isEmpty()) {
            long activeDoctors = userRepository.countByClinicIdAndRoleAndIsActiveTrueAndIsDeletedFalse(clinicId, UserRole.DOCTOR);
            if (activeDoctors <= 1) {
                calls = callRecordRepository.findCallsByClinicAndDateRange(clinicId, rangeStart, rangeEnd);
            }
        }

        List<Appointment> appointments = appointmentRepository.findAppointmentsByClinicDoctorAndDateRange(
                clinicId, doctorId, rangeStart, rangeEnd);

        List<DailyMetric> result = new ArrayList<>();
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            final LocalDate dateKey = cur;

            int dayCalls = 0;
            int dayAiCalls = 0;
            int dayHumanTransfers = 0;
            for (CallRecord cr : calls) {
                LocalDate callDate = cr.getCallStartedAt() != null ? cr.getCallStartedAt().toLocalDate()
                        : (cr.getCreatedOn() != null ? cr.getCreatedOn().toLocalDate() : null);
                if (dateKey.equals(callDate)) {
                    dayCalls++;
                    if (cr.getCallType() == CallType.AI_INBOUND) {
                        dayAiCalls++;
                    }
                    if (Boolean.TRUE.equals(cr.getTransferredToHuman())) {
                        dayHumanTransfers++;
                    }
                }
            }

            int dayAiBookings = 0;
            int daySuccessful = 0;
            int dayNewLeads = 0;
            for (Appointment appt : appointments) {
                LocalDate apptDate = appt.getCreatedOn() != null ? appt.getCreatedOn().toLocalDate()
                        : (appt.getAppointmentStart() != null ? appt.getAppointmentStart().toLocalDate() : null);
                if (dateKey.equals(apptDate)) {
                    if (appt.getBookingSource() == BookingSource.AI_AGENT || appt.getCallRecord() != null) {
                        dayAiBookings++;
                    }
                    if (appt.getStatus() == AppointmentStatus.SCHEDULED
                            || appt.getStatus() == AppointmentStatus.CONFIRMED
                            || appt.getStatus() == AppointmentStatus.COMPLETED) {
                        daySuccessful++;
                    }
                    if (appt.getPatient() != null && Boolean.TRUE.equals(appt.getPatient().getIsNewLead())) {
                        dayNewLeads++;
                    }
                }
            }

            result.add(DailyMetric.builder()
                    .date(dateKey.toString())
                    .callsReceived(dayCalls)
                    .appointmentsBookedViaCallAgent(dayAiBookings)
                    .totalAiCalls(dayAiCalls)
                    .totalHumanTransfers(dayHumanTransfers)
                    .successfulBookings(daySuccessful)
                    .newLeadBookings(dayNewLeads)
                    .build());

            cur = cur.plusDays(1);
        }

        return result;
    }

    private String normalizeFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return "DAY";
        }
        String upper = filter.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "WEEK", "THIS_WEEK" -> "WEEK";
            case "MONTH", "THIS_MONTH" -> "MONTH";
            case "CUSTOM", "RANGE" -> "CUSTOM";
            default -> "DAY";
        };
    }

    private DateRange resolveDateRange(String filter, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        return switch (filter) {
            case "WEEK" -> {
                LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                yield new DateRange(start, end);
            }
            case "MONTH" -> {
                LocalDate start = today.withDayOfMonth(1);
                LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
                yield new DateRange(start, end);
            }
            case "CUSTOM" -> {
                LocalDate start = startDate != null ? startDate : (endDate != null ? endDate : today);
                LocalDate end = endDate != null ? endDate : start;
                if (start.isAfter(end)) {
                    LocalDate tmp = start;
                    start = end;
                    end = tmp;
                }
                yield new DateRange(start, end);
            }
            default -> new DateRange(today, today);
        };
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime() {
            return startDate.atStartOfDay();
        }

        LocalDateTime endDateTime() {
            return endDate.atTime(LocalTime.MAX);
        }
    }
}
