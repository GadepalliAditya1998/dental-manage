package com.dental.manage.service.provider;

import com.dental.manage.entity.Appointment;

/**
 * Provider interface for sending WhatsApp messages and reminders.
 * Decouples WhatsApp business logic from the specific SMS/WhatsApp gateway (e.g., Twilio).
 */
public interface WhatsAppProvider {

    /**
     * Dispatches a raw text message to the specified recipient phone number.
     *
     * @param toPhoneNumber Recipient phone number (e.g. +1234567890 or whatsapp:+1234567890)
     * @param messageText Text content of the WhatsApp message
     * @return true if successfully dispatched, false otherwise
     */
    boolean sendMessage(String toPhoneNumber, String messageText);

    /**
     * Formats and dispatches a structured appointment reminder WhatsApp message for an upcoming appointment.
     * Presents the patient with interactive reply options:
     * 1 - Yes, I am available
     * 2 - Reschedule appointment
     * 3 - Cancel
     * 4 - Don't send these messages
     *
     * @param appointment The scheduled appointment entity with patient, doctor, and service details
     * @return true if successfully sent, false otherwise
     */
    boolean sendAppointmentReminder(Appointment appointment);

    /**
     * Normalizes a phone number to standard E.164 WhatsApp format (e.g., "whatsapp:+15551234567").
     * Defaults to US (+1) country code if not explicitly present.
     *
     * @param rawPhone Raw phone number string
     * @return Formatted WhatsApp phone number
     */
    String formatWhatsAppNumber(String rawPhone);
}
