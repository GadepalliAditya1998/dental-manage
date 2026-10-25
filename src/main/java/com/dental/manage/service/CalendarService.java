package com.dental.manage.service;

import com.dental.manage.entity.Appointment;

import java.util.Optional;

/**
 * Service contract for calendar integrations (e.g. Google Calendar).
 * Synchronizes appointment creations, updates, and cancellations across doctor and patient calendars.
 */
public interface CalendarService {

    /**
     * Creates a calendar event for the given appointment.
     * Invites both the patient and the doctor as attendees with automated invites.
     *
     * @param appointment the scheduled appointment entity
     * @return Optional containing the generated calendar event ID, or empty if disabled / unavailable
     */
    Optional<String> createAppointmentEvent(Appointment appointment);

    /**
     * Updates an existing calendar event for the given appointment.
     * If the appointment does not yet have a calendar event ID, this will create one.
     * Updates times, doctor/patient attendees, service, status, and notes.
     *
     * @param appointment the updated appointment entity
     * @return true if successfully updated or created, false otherwise
     */
    boolean updateAppointmentEvent(Appointment appointment);

    /**
     * Cancels the calendar event for the given appointment, notifying attendees.
     *
     * @param appointment the cancelled appointment entity
     * @param reason optional reason for cancellation
     * @return true if successfully cancelled, false otherwise
     */
    boolean cancelAppointmentEvent(Appointment appointment, String reason);

    /**
     * Forces synchronization or re-synchronization of an appointment event.
     *
     * @param appointment the appointment to synchronize
     * @return Optional containing the calendar event ID
     */
    Optional<String> syncAppointmentEvent(Appointment appointment);

    /**
     * Checks if calendar synchronization is configured and active.
     *
     * @return true if enabled and configured
     */
    boolean isEnabled();
}
