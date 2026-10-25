package com.dental.manage.service.provider.impl;

import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.Patient;
import com.dental.manage.entity.Service;
import com.dental.manage.service.provider.WhatsAppProvider;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Twilio concrete implementation of the WhatsAppProvider interface.
 * Formats WhatsApp messages and uses the Twilio REST API to dispatch them.
 */
@Component
@Slf4j
public class TwilioWhatsAppProvider implements WhatsAppProvider {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.whatsapp-number:}")
    private String twilioWhatsAppNumber;

    @Value("${clinic.name:Smile Dental Clinic}")
    private String clinicName;

    @Value("${clinic.country-code:+1}")
    private String defaultCountryCode;

    @Override
    public boolean sendMessage(String toPhoneNumber, String messageText) {
        if (toPhoneNumber == null || toPhoneNumber.isBlank()) {
            log.warn("Recipient phone number is missing. Cannot send WhatsApp message.");
            return false;
        }

        if (messageText == null || messageText.isBlank()) {
            log.warn("Message text is empty. Cannot send WhatsApp message to {}", toPhoneNumber);
            return false;
        }

        if (!isTwilioConfigured()) {
            log.warn("Twilio credentials or WhatsApp sender number not configured. Message to {} skipped: \n{}",
                    toPhoneNumber, messageText);
            return false;
        }

        String formattedTo = formatWhatsAppNumber(toPhoneNumber);
        String formattedFrom = formatSenderNumber(twilioWhatsAppNumber);

        try {
            log.info("Dispatching WhatsApp message from {} to {}", formattedFrom, formattedTo);
            Message message = Message.creator(
                    new PhoneNumber(formattedTo),
                    new PhoneNumber(formattedFrom),
                    messageText
            ).create();

            log.info("WhatsApp message successfully sent. Message SID: {}, Status: {}",
                    message.getSid(), message.getStatus());
            return true;
        } catch (Exception ex) {
            log.error("Failed to send WhatsApp message via Twilio to {}: {}", formattedTo, ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public boolean sendAppointmentReminder(Appointment appointment) {
        if (appointment == null) {
            log.warn("Cannot send reminder: Appointment is null");
            return false;
        }

        Patient patient = appointment.getPatient();
        if (patient == null || patient.getPhoneNumber() == null || patient.getPhoneNumber().isBlank()) {
            log.warn("Cannot send reminder for appointment ID {}: Missing patient or phone number",
                    appointment.getId());
            return false;
        }

        if (Boolean.TRUE.equals(patient.getWhatsappOptOut())) {
            log.info("Patient ID {} has opted out of WhatsApp messages. Skipping appointment reminder {}.",
                    patient.getId(), appointment.getId());
            return false;
        }

        String messageBody = buildReminderMessage(appointment);
        return sendMessage(patient.getPhoneNumber(), messageBody);
    }

    @Override
    public String formatWhatsAppNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }

        String cleaned = rawPhone.trim();
        if (cleaned.startsWith("whatsapp:")) {
            cleaned = cleaned.substring("whatsapp:".length()).trim();
        }

        // Remove non-digit characters except leading '+'
        boolean hasLeadingPlus = cleaned.startsWith("+");
        String digitsOnly = cleaned.replaceAll("[^0-9]", "");

        String e164;
        if (hasLeadingPlus) {
            e164 = "+" + digitsOnly;
        } else if (digitsOnly.length() == 10) {
            // Standard US 10-digit number without country code
            e164 = "+1" + digitsOnly;
        } else if (digitsOnly.length() == 11 && digitsOnly.startsWith("1")) {
            // US 11-digit number starting with 1
            e164 = "+" + digitsOnly;
        } else {
            // Fallback to configured default country code
            String prefix = defaultCountryCode.startsWith("+") ? defaultCountryCode : "+" + defaultCountryCode;
            e164 = prefix + digitsOnly;
        }

        return "whatsapp:" + e164;
    }

    private String formatSenderNumber(String sender) {
        if (sender == null || sender.isBlank()) {
            return "";
        }
        String cleaned = sender.trim();
        return cleaned.startsWith("whatsapp:") ? cleaned : "whatsapp:" + cleaned;
    }

    private boolean isTwilioConfigured() {
        return accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && twilioWhatsAppNumber != null && !twilioWhatsAppNumber.isBlank();
    }

    private String buildReminderMessage(Appointment appointment) {
        Patient patient = appointment.getPatient();
        Doctor doctor = appointment.getDoctor();
        Service service = appointment.getService();

        String patientName = (patient.getFirstName() != null) ? patient.getFirstName() : "there";
        String dateFormatted = (appointment.getAppointmentStart() != null)
                ? appointment.getAppointmentStart().format(DATE_FORMATTER)
                : "tomorrow";
        String timeFormatted = (appointment.getAppointmentStart() != null)
                ? appointment.getAppointmentStart().format(TIME_FORMATTER)
                : "";

        String doctorName = "";
        if (doctor != null) {
            doctorName = "Dr. " + (doctor.getFirstName() != null ? doctor.getFirstName() : "")
                    + (doctor.getLastName() != null ? " " + doctor.getLastName() : "");
            doctorName = doctorName.trim();
        }

        String serviceName = (service != null && service.getName() != null) ? service.getName() : "Dental Consultation";

        return String.format(
                "Hello %s,\n\n" +
                "This is a reminder for your dental appointment scheduled for tomorrow with %s:\n\n" +
                "📅 Date: %s\n" +
                "⏰ Time: %s\n" +
                "👨‍⚕️ Doctor: %s\n" +
                "🩺 Service: %s\n\n" +
                "Please confirm if you are attending by replying with a number:\n" +
                "1️⃣ Yes, I am available\n" +
                "2️⃣ Reschedule appointment\n" +
                "3️⃣ Cancel\n" +
                "4️⃣ Don't send these messages\n\n" +
                "Reply with 1, 2, 3, or 4 (or reply YES, RESCHEDULE, CANCEL, STOP).",
                patientName,
                clinicName,
                dateFormatted,
                timeFormatted,
                doctorName,
                serviceName
        );
    }
}
