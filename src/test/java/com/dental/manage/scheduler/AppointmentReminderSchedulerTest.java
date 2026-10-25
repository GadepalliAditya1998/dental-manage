package com.dental.manage.scheduler;

import com.dental.manage.service.AppointmentReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderSchedulerTest {

    @Mock
    private AppointmentReminderService reminderService;

    @InjectMocks
    private AppointmentReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "reminderEnabled", true);
    }

    @Test
    @DisplayName("runDailyAppointmentReminders executes reminder service when enabled")
    void shouldExecuteWhenEnabled() {
        when(reminderService.sendUpcomingAppointmentReminders()).thenReturn(3);

        scheduler.runDailyAppointmentReminders();

        verify(reminderService).sendUpcomingAppointmentReminders();
    }

    @Test
    @DisplayName("runDailyAppointmentReminders skips execution when reminderEnabled is false")
    void shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "reminderEnabled", false);

        scheduler.runDailyAppointmentReminders();

        verify(reminderService, never()).sendUpcomingAppointmentReminders();
    }

    @Test
    @DisplayName("runDailyAppointmentReminders catches exceptions gracefully without throwing")
    void shouldHandleExceptionGracefully() {
        when(reminderService.sendUpcomingAppointmentReminders()).thenThrow(new RuntimeException("Simulated error"));

        // Should not throw
        scheduler.runDailyAppointmentReminders();

        verify(reminderService).sendUpcomingAppointmentReminders();
    }
}
