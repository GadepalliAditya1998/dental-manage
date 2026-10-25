package com.dental.manage.service;

import com.dental.manage.dto.response.DashboardMetricsResponse;
import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.CallRecord;
import com.dental.manage.entity.Doctor;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.BookingSource;
import com.dental.manage.enums.CallType;
import com.dental.manage.enums.UserRole;
import com.dental.manage.repository.AppointmentRepository;
import com.dental.manage.repository.CallRecordRepository;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private CallRecordRepository callRecordRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Long clinicId = 1L;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("getMetrics should return DAY metrics for receptionist when doctorId is null")
    void getMetrics_dayFilter_receptionist_shouldSucceed() {
        LocalDate today = LocalDate.now();

        when(callRecordRepository.countCallsByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(15L);
        when(appointmentRepository.countAiAgentBookingsByDateRange(eq(clinicId), eq(null), eq(BookingSource.AI_AGENT), any(), any())).thenReturn(7L);
        when(callRecordRepository.countAiCallsByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(14L);
        when(callRecordRepository.countHumanTransfersByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(2L);
        when(appointmentRepository.countSuccessfulBookingsByDateRange(eq(clinicId), eq(null), any(), any())).thenReturn(10L);
        when(appointmentRepository.countNewLeadBookingsByDateRange(eq(clinicId), eq(null), any(), any())).thenReturn(3L);
        when(appointmentRepository.countByStatusClinicDoctorAndDateRange(eq(clinicId), eq(null), eq(AppointmentStatus.CANCELLED), any(), any())).thenReturn(1L);
        when(appointmentRepository.countByStatusClinicDoctorAndDateRange(eq(clinicId), eq(null), eq(AppointmentStatus.RESCHEDULED), any(), any())).thenReturn(0L);
        when(callRecordRepository.avgDurationByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(125000.0);
        when(callRecordRepository.avgSentimentByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(0.85);

        when(callRecordRepository.findCallsByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(Collections.emptyList());
        when(appointmentRepository.findAppointmentsByClinicDoctorAndDateRange(eq(clinicId), eq(null), any(), any())).thenReturn(Collections.emptyList());

        DashboardMetricsResponse response = dashboardService.getMetrics(clinicId, null, "DAY", null, null);

        assertThat(response).isNotNull();
        assertThat(response.getFilter()).isEqualTo("DAY");
        assertThat(response.getStartDate()).isEqualTo(today.toString());
        assertThat(response.getEndDate()).isEqualTo(today.toString());
        assertThat(response.getCallsReceived()).isEqualTo(15L);
        assertThat(response.getAppointmentsBookedViaCallAgent()).isEqualTo(7L);
        assertThat(response.getTotalHumanTransfers()).isEqualTo(2L);
        assertThat(response.getSuccessfulBookings()).isEqualTo(10L);
        assertThat(response.getAvgCallDurationSec()).isEqualTo(new BigDecimal("125.00"));
        assertThat(response.getAvgSentimentScore()).isEqualTo(new BigDecimal("0.85"));
        assertThat(response.getDailyMetrics()).hasSize(1);
    }

    @Test
    @DisplayName("getMetrics should return WEEK metrics with daily breakdown")
    void getMetrics_weekFilter_shouldSucceed() {
        when(callRecordRepository.countCallsByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(50L);
        when(appointmentRepository.countAiAgentBookingsByDateRange(eq(clinicId), eq(null), eq(BookingSource.AI_AGENT), any(), any())).thenReturn(25L);
        when(callRecordRepository.findCallsByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(Collections.emptyList());
        when(appointmentRepository.findAppointmentsByClinicDoctorAndDateRange(eq(clinicId), eq(null), any(), any())).thenReturn(Collections.emptyList());

        DashboardMetricsResponse response = dashboardService.getMetrics(clinicId, null, "WEEK", null, null);

        assertThat(response).isNotNull();
        assertThat(response.getFilter()).isEqualTo("WEEK");
        assertThat(response.getCallsReceived()).isEqualTo(50L);
        assertThat(response.getAppointmentsBookedViaCallAgent()).isEqualTo(25L);
        assertThat(response.getDailyMetrics()).hasSize(7);
    }

    @Test
    @DisplayName("getMetrics should return MONTH metrics")
    void getMetrics_monthFilter_shouldSucceed() {
        when(callRecordRepository.countCallsByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(200L);
        when(appointmentRepository.countAiAgentBookingsByDateRange(eq(clinicId), eq(null), eq(BookingSource.AI_AGENT), any(), any())).thenReturn(80L);
        when(callRecordRepository.findCallsByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(Collections.emptyList());
        when(appointmentRepository.findAppointmentsByClinicDoctorAndDateRange(eq(clinicId), eq(null), any(), any())).thenReturn(Collections.emptyList());

        DashboardMetricsResponse response = dashboardService.getMetrics(clinicId, null, "MONTH", null, null);

        assertThat(response).isNotNull();
        assertThat(response.getFilter()).isEqualTo("MONTH");
        assertThat(response.getCallsReceived()).isEqualTo(200L);
        assertThat(response.getAppointmentsBookedViaCallAgent()).isEqualTo(80L);
        assertThat(response.getDailyMetrics().size()).isGreaterThanOrEqualTo(28);
    }

    @Test
    @DisplayName("getMetrics should return CUSTOM date range metrics")
    void getMetrics_customFilter_shouldSucceed() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();

        when(callRecordRepository.countCallsByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(30L);
        when(appointmentRepository.countAiAgentBookingsByDateRange(eq(clinicId), eq(null), eq(BookingSource.AI_AGENT), any(), any())).thenReturn(12L);
        when(callRecordRepository.findCallsByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(Collections.emptyList());
        when(appointmentRepository.findAppointmentsByClinicDoctorAndDateRange(eq(clinicId), eq(null), any(), any())).thenReturn(Collections.emptyList());

        DashboardMetricsResponse response = dashboardService.getMetrics(clinicId, null, "CUSTOM", start, end);

        assertThat(response).isNotNull();
        assertThat(response.getFilter()).isEqualTo("CUSTOM");
        assertThat(response.getStartDate()).isEqualTo(start.toString());
        assertThat(response.getEndDate()).isEqualTo(end.toString());
        assertThat(response.getCallsReceived()).isEqualTo(30L);
        assertThat(response.getAppointmentsBookedViaCallAgent()).isEqualTo(12L);
        assertThat(response.getDailyMetrics()).hasSize(6);
    }

    @Test
    @DisplayName("getMetrics should scope calls and bookings to doctorId when provided")
    void getMetrics_doctorScope_shouldSucceed() {
        Long doctorId = 10L;

        when(callRecordRepository.countCallsByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(40L);
        when(callRecordRepository.countCallsByDoctorAndDateRange(eq(clinicId), eq(doctorId), any(), any())).thenReturn(18L);
        when(appointmentRepository.countAiAgentBookingsByDateRange(eq(clinicId), eq(doctorId), eq(BookingSource.AI_AGENT), any(), any())).thenReturn(8L);
        when(appointmentRepository.countSuccessfulBookingsByDateRange(eq(clinicId), eq(doctorId), any(), any())).thenReturn(12L);
        when(appointmentRepository.countNewLeadBookingsByDateRange(eq(clinicId), eq(doctorId), any(), any())).thenReturn(4L);

        when(callRecordRepository.findCallsByDoctorAndDateRange(eq(clinicId), eq(doctorId), any(), any())).thenReturn(Collections.emptyList());
        when(appointmentRepository.findAppointmentsByClinicDoctorAndDateRange(eq(clinicId), eq(doctorId), any(), any())).thenReturn(Collections.emptyList());

        DashboardMetricsResponse response = dashboardService.getMetrics(clinicId, doctorId, "DAY", null, null);

        assertThat(response).isNotNull();
        assertThat(response.getDoctorId()).isEqualTo(doctorId);
        assertThat(response.getCallsReceived()).isEqualTo(18L);
        assertThat(response.getAppointmentsBookedViaCallAgent()).isEqualTo(8L);
        assertThat(response.getClinicTotalCalls()).isEqualTo(40L);
        assertThat(response.getSuccessfulBookings()).isEqualTo(12L);
        assertThat(response.getNewLeadBookings()).isEqualTo(4L);
    }

    @Test
    @DisplayName("getMetrics for doctor with 0 specific calls in single-doctor clinic should show clinic calls")
    void getMetrics_doctorScope_solePractitionerFallback_shouldSucceed() {
        Long doctorId = 10L;

        when(callRecordRepository.countCallsByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(22L);
        when(callRecordRepository.countCallsByDoctorAndDateRange(eq(clinicId), eq(doctorId), any(), any())).thenReturn(0L);
        when(userRepository.countByClinicIdAndRoleAndIsActiveTrueAndIsDeletedFalse(clinicId, UserRole.DOCTOR)).thenReturn(1L);
        when(appointmentRepository.countAiAgentBookingsByDateRange(eq(clinicId), eq(doctorId), eq(BookingSource.AI_AGENT), any(), any())).thenReturn(6L);

        when(callRecordRepository.findCallsByDoctorAndDateRange(eq(clinicId), eq(doctorId), any(), any())).thenReturn(Collections.emptyList());
        when(callRecordRepository.findCallsByClinicAndDateRange(eq(clinicId), any(), any())).thenReturn(Collections.emptyList());
        when(appointmentRepository.findAppointmentsByClinicDoctorAndDateRange(eq(clinicId), eq(doctorId), any(), any())).thenReturn(Collections.emptyList());

        DashboardMetricsResponse response = dashboardService.getMetrics(clinicId, doctorId, "DAY", null, null);

        assertThat(response).isNotNull();
        assertThat(response.getDoctorId()).isEqualTo(doctorId);
        assertThat(response.getCallsReceived()).isEqualTo(22L);
        assertThat(response.getAppointmentsBookedViaCallAgent()).isEqualTo(6L);
    }
}
