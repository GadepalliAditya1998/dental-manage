package com.dental.manage.controller;

import com.dental.manage.dto.request.ClinicSettingsRequest;
import com.dental.manage.dto.response.ClinicSettingsResponse;
import com.dental.manage.dto.response.ClinicWorkingHoursResponse;
import com.dental.manage.exception.GlobalExceptionHandler;
import com.dental.manage.service.ClinicDetailsService;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClinicDetailsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClinicDetailsService clinicDetailsService;

    @InjectMocks
    private ClinicDetailsController clinicDetailsController;

    private ClinicWorkingHoursResponse weekResponse;
    private ClinicWorkingHoursResponse dayResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clinicDetailsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        ClinicWorkingHoursResponse.DayWorkingHours monday = ClinicWorkingHoursResponse.DayWorkingHours.builder()
                .dayOfWeek("MONDAY")
                .dateTime(LocalDateTime.of(2026, 10, 19, 0, 0))
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(18, 0))
                .isOpen(true)
                .status("OPEN")
                .formattedHours("09:00 AM - 06:00 PM")
                .build();

        ClinicWorkingHoursResponse.DayWorkingHours sunday = ClinicWorkingHoursResponse.DayWorkingHours.builder()
                .dayOfWeek("SUNDAY")
                .dateTime(LocalDateTime.of(2026, 10, 25, 0, 0))
                .isOpen(false)
                .status("CLOSED")
                .formattedHours("Closed")
                .build();

        weekResponse = ClinicWorkingHoursResponse.builder()
                .id(1L)
                .clinicName("Smile Dental Clinic")
                .clinicPhone("+1234567890")
                .receptionistPhone("+1987654321")
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(18, 0))
                .workingDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY")
                .timezone("Asia/Kolkata")
                .retellAgentId("agent_123")
                .period("WEEK")
                .workingHours(List.of(monday, sunday))
                .build();

        dayResponse = ClinicWorkingHoursResponse.builder()
                .id(1L)
                .clinicName("Smile Dental Clinic")
                .clinicPhone("+1234567890")
                .period("DAY")
                .workingHours(List.of(monday))
                .build();
    }

    @Test
    @DisplayName("GET /api/clinic with period=week should return weekly working hours")
    void getClinicDetails_week_shouldReturnWeeklySchedule() throws Exception {
        when(clinicDetailsService.getClinicDetailsWithWorkingHours(eq("week"), any(), any()))
                .thenReturn(weekResponse);

        mockMvc.perform(get("/api/clinic")
                        .param("period", "week")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.clinicName", is("Smile Dental Clinic")))
                .andExpect(jsonPath("$.data.period", is("WEEK")))
                .andExpect(jsonPath("$.data.workingHours", hasSize(2)))
                .andExpect(jsonPath("$.data.workingHours[0].dayOfWeek", is("MONDAY")))
                .andExpect(jsonPath("$.data.workingHours[0].isOpen", is(true)))
                .andExpect(jsonPath("$.data.workingHours[0].status", is("OPEN")));
    }

    @Test
    @DisplayName("GET /api/clinic with period=day should return day working hours")
    void getClinicDetails_day_shouldReturnDaySchedule() throws Exception {
        when(clinicDetailsService.getClinicDetailsWithWorkingHours(eq("day"), any(), any()))
                .thenReturn(dayResponse);

        mockMvc.perform(get("/api/clinic")
                        .param("period", "day")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.period", is("DAY")))
                .andExpect(jsonPath("$.data.workingHours", hasSize(1)))
                .andExpect(jsonPath("$.data.workingHours[0].dayOfWeek", is("MONDAY")));
    }

    @Test
    @DisplayName("GET /api/clinic with view=day alias should resolve correctly")
    void getClinicDetails_viewAlias_shouldResolveCorrectly() throws Exception {
        when(clinicDetailsService.getClinicDetailsWithWorkingHours(eq("day"), any(), any()))
                .thenReturn(dayResponse);

        mockMvc.perform(get("/api/clinic")
                        .param("view", "day")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period", is("DAY")));
    }

    @Test
    @DisplayName("GET /api/clinic/day should invoke getClinicDetailsForDay")
    void getClinicDetailsForDay_shouldReturnDaySchedule() throws Exception {
        when(clinicDetailsService.getClinicDetailsForDay(any())).thenReturn(dayResponse);

        mockMvc.perform(get("/api/clinic/day")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period", is("DAY")))
                .andExpect(jsonPath("$.data.workingHours", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/clinic/week should invoke getClinicDetailsForWeek")
    void getClinicDetailsForWeek_shouldReturnWeekSchedule() throws Exception {
        when(clinicDetailsService.getClinicDetailsForWeek(any())).thenReturn(weekResponse);

        mockMvc.perform(get("/api/clinic/week")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period", is("WEEK")))
                .andExpect(jsonPath("$.data.workingHours", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/clinic/settings should return clinic settings")
    void getClinicSettings_shouldReturnSettings() throws Exception {
        ClinicSettingsResponse settings = ClinicSettingsResponse.builder()
                .id(1L)
                .clinicName("Smile Dental Clinic")
                .clinicPhone("+1234567890")
                .timezone("Asia/Kolkata")
                .build();

        when(clinicDetailsService.getClinicSettings()).thenReturn(settings);

        mockMvc.perform(get("/api/clinic/settings")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clinicName", is("Smile Dental Clinic")))
                .andExpect(jsonPath("$.data.timezone", is("Asia/Kolkata")));
    }

    @Test
    @DisplayName("PUT /api/clinic should update and return clinic configuration")
    void updateClinicDetails_shouldUpdateSettings() throws Exception {
        ClinicSettingsResponse updated = ClinicSettingsResponse.builder()
                .id(1L)
                .clinicName("Smile Dental Premium")
                .build();

        when(clinicDetailsService.updateClinicDetails(any(ClinicSettingsRequest.class))).thenReturn(updated);

        String jsonPayload = """
                {
                    "clinicName": "Smile Dental Premium",
                    "clinicPhone": "+1234567890"
                }
                """;

        mockMvc.perform(put("/api/clinic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clinicName", is("Smile Dental Premium")));

        verify(clinicDetailsService).updateClinicDetails(any(ClinicSettingsRequest.class));
    }
}
