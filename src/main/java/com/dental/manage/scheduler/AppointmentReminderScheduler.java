package com.dental.manage.scheduler;

import com.dental.manage.service.AppointmentReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled CRON job that triggers automated patient WhatsApp follow-up reminders.
 * Runs on the scheduled CRON expression configured in clinic.reminder.cron.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final AppointmentReminderService reminderService;

    @Value("${clinic.reminder.enabled:true}")
    private boolean reminderEnabled;

    /**
     * Executes the daily appointment reminder job.
     * Uses the configured clinic cron expression and timezone.
     */
    @Scheduled(cron = "${clinic.reminder.cron:0 0 9 * * ?}", zone = "${clinic.timezone:Asia/Kolkata}")
    public void runDailyAppointmentReminders() {
        if (!reminderEnabled) {
            log.info("Appointment reminder scheduler is disabled by configuration (clinic.reminder.enabled=false).");
            return;
        }

        log.info("==== STARTING DAILY APPOINTMENT REMINDER CRON JOB ====");
        try {
            int sentCount = reminderService.sendUpcomingAppointmentReminders();
            log.info("==== COMPLETED DAILY APPOINTMENT REMINDER CRON JOB. Processed {} reminders ====", sentCount);
        } catch (Exception e) {
            log.error("Unhandled error during daily appointment reminder CRON job execution: {}", e.getMessage(), e);
        }
    }
}
