package com.dental.manage.service;

import java.time.LocalDate;

/**
 * Service for identifying upcoming appointments and dispatching reminders to patients.
 */
public interface AppointmentReminderService {

    /**
     * Finds all appointments scheduled for tomorrow (or configured days-before window)
     * and sends WhatsApp confirmation reminders to non-opted-out patients.
     *
     * @return Number of reminder messages successfully dispatched
     */
    int sendUpcomingAppointmentReminders();

    /**
     * Finds all appointments scheduled on the specified target date and dispatches reminders.
     *
     * @param targetDate The date of appointments to remind patients about
     * @return Number of reminder messages successfully dispatched
     */
    int sendRemindersForDate(LocalDate targetDate);
}
