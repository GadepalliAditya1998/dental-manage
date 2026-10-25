package com.dental.manage.controller;

import com.dental.manage.dto.response.DashboardMetricsResponse;
import com.dental.manage.exception.GlobalExceptionHandler;
import com.dental.manage.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    private DashboardMetricsResponse mockMetrics;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMetrics = DashboardMetricsResponse.builder()
                .clinicId(1L)
                .filter("DAY")
                .startDate("2026-10-25")
                .endDate("2026-10-25")
                .callsReceived(15L)
                .appointmentsBookedViaCallAgent(8L)
                .clinicTotalCalls(15L)
                .totalAiCalls(14L)
                .totalHumanTransfers(1L)
                .successfulBookings(10L)
                .newLeadBookings(3L)
                .cancelledAppointments(1L)
                .rescheduledAppointments(0L)
                .avgCallDurationSec(new BigDecimal("130.00"))
                .avgSentimentScore(new BigDecimal("0.90"))
                .dailyMetrics(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("GET /api/dashboard/metrics should return metrics with 200 OK")
    void getMetrics_defaultParams_shouldReturnSuccess() throws Exception {
        when(dashboardService.getMetrics(eq(1L), eq(null), eq("DAY"), any(), any()))
                .thenReturn(mockMetrics);

        mockMvc.perform(get("/api/dashboard/metrics")
                        .param("clinicId", "1")
                        .param("filter", "DAY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.callsReceived", is(15)))
                .andExpect(jsonPath("$.data.appointmentsBookedViaCallAgent", is(8)))
                .andExpect(jsonPath("$.data.filter", is("DAY")))
                .andExpect(jsonPath("$.data.successfulBookings", is(10)));
    }

    @Test
    @DisplayName("GET /api/dashboard/metrics with doctorId should return doctor scoped metrics")
    void getMetrics_withDoctorId_shouldReturnDoctorScopedMetrics() throws Exception {
        DashboardMetricsResponse doctorMetrics = DashboardMetricsResponse.builder()
                .clinicId(1L)
                .doctorId(10L)
                .filter("WEEK")
                .callsReceived(9L)
                .appointmentsBookedViaCallAgent(5L)
                .successfulBookings(7L)
                .build();

        when(dashboardService.getMetrics(eq(1L), eq(10L), eq("WEEK"), any(), any()))
                .thenReturn(doctorMetrics);

        mockMvc.perform(get("/api/dashboard/metrics")
                        .param("clinicId", "1")
                        .param("doctorId", "10")
                        .param("filter", "WEEK")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.doctorId", is(10)))
                .andExpect(jsonPath("$.data.callsReceived", is(9)))
                .andExpect(jsonPath("$.data.appointmentsBookedViaCallAgent", is(5)));
    }

    @Test
    @DisplayName("GET /api/dashboard/metrics with CUSTOM filter and dates")
    void getMetrics_customDates_shouldReturnSuccess() throws Exception {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 15);

        DashboardMetricsResponse customMetrics = DashboardMetricsResponse.builder()
                .clinicId(1L)
                .filter("CUSTOM")
                .startDate(start.toString())
                .endDate(end.toString())
                .callsReceived(45L)
                .appointmentsBookedViaCallAgent(20L)
                .build();

        when(dashboardService.getMetrics(eq(1L), eq(null), eq("CUSTOM"), eq(start), eq(end)))
                .thenReturn(customMetrics);

        mockMvc.perform(get("/api/dashboard/metrics")
                        .param("clinicId", "1")
                        .param("filter", "CUSTOM")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-15")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.filter", is("CUSTOM")))
                .andExpect(jsonPath("$.data.callsReceived", is(45)))
                .andExpect(jsonPath("$.data.appointmentsBookedViaCallAgent", is(20)));
    }

    @Test
    @DisplayName("GET /api/dashboard/daily should return daily metrics breakdown")
    void getDailyMetrics_shouldReturnSuccess() throws Exception {
        when(dashboardService.getMetrics(eq(1L), eq(null), eq("WEEK"), any(), any()))
                .thenReturn(mockMetrics);

        mockMvc.perform(get("/api/dashboard/daily")
                        .param("clinicId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.callsReceived", is(15)));
    }
}
