package com.dental.manage.controller;

import com.dental.manage.service.AppointmentReminderService;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderControllerTest {

    @Mock
    private AppointmentReminderService reminderService;

    @InjectMocks
    private AppointmentReminderController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/reminders/trigger without date triggers upcoming appointment reminders")
    void shouldTriggerUpcomingReminders() throws Exception {
        when(reminderService.sendUpcomingAppointmentReminders()).thenReturn(5);

        mockMvc.perform(post("/api/reminders/trigger")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.remindersSent", is(5)));

        verify(reminderService).sendUpcomingAppointmentReminders();
    }

    @Test
    @DisplayName("POST /api/reminders/trigger?date=2026-11-20 triggers reminders for specified date")
    void shouldTriggerRemindersForSpecifiedDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 20);
        when(reminderService.sendRemindersForDate(date)).thenReturn(2);

        mockMvc.perform(post("/api/reminders/trigger")
                        .param("date", "2026-11-20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.remindersSent", is(2)))
                .andExpect(jsonPath("$.targetDate", is("2026-11-20")));

        verify(reminderService).sendRemindersForDate(date);
    }
}
