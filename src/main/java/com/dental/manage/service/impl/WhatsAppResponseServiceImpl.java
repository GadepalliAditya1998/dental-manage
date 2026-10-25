package com.dental.manage.service.impl;

import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.Patient;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.ConfirmationStatus;
import com.dental.manage.repository.AppointmentRepository;
import com.dental.manage.repository.PatientRepository;
import com.dental.manage.service.CalendarService;
import com.dental.manage.service.WhatsAppResponseService;
import com.dental.manage.service.provider.WhatsAppProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Service to process inbound WhatsApp messages from patients and take appropriate actions:
 * - Option 1: Confirm appointment
 * - Option 2: Request rescheduling
 * - Option 3: Cancel appointment
 * - Option 4: Opt out of future messages
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppResponseServiceImpl implements WhatsAppResponseService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final WhatsAppProvider whatsAppProvider;
    private final CalendarService calendarService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    @Value("${clinic.name:Smile Dental Clinic}")
    private String clinicName;

    @Value("${clinic.phone:+1-800-555-0199}")
    private String clinicPhone;

    @Override
    @Transactional
    public String processInboundMessage(String fromPhone, String messageBody) {
        log.info("Processing inbound WhatsApp message from: '{}', body: '{}'", fromPhone, messageBody);

        if (fromPhone == null || fromPhone.isBlank()) {
            log.warn("Cannot process message: fromPhone is empty");
            return "Phone number missing.";
        }

        String normalizedPhone = cleanPhoneNumber(fromPhone);
        String trimmedBody = (messageBody != null) ? messageBody.trim().toLowerCase() : "";

        // Find upcoming appointment for patient
        Appointment appointment = findUpcomingAppointment(normalizedPhone);
        Patient patient = (appointment != null) ? appointment.getPatient() : findPatient(normalizedPhone);

        String replyText;

        if (isConfirmChoice(trimmedBody)) {
            replyText = handleConfirm(appointment, patient);
        } else if (isRescheduleChoice(trimmedBody)) {
            replyText = handleReschedule(appointment, patient);
        } else if (isCancelChoice(trimmedBody)) {
            replyText = handleCancel(appointment, patient);
        } else if (isOptOutChoice(trimmedBody)) {
            replyText = handleOptOut(patient, true);
        } else if (isOptInChoice(trimmedBody)) {
            replyText = handleOptOut(patient, false);
        } else {
            replyText = handleUnrecognizedInput();
        }

        // Send auto-reply to patient via provider
        whatsAppProvider.sendMessage(fromPhone, replyText);
        return replyText;
    }

    private String handleConfirm(Appointment appointment, Patient patient) {
        if (appointment == null) {
            return String.format(
                    "Thank you for contacting %s! We could not locate an upcoming pending appointment for this phone number. " +
                    "If you need assistance, please call us at %s.",
                    clinicName, clinicPhone);
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setConfirmationStatus(ConfirmationStatus.CONFIRMED);
        appointmentRepository.save(appointment);

        try {
            calendarService.updateAppointmentEvent(appointment);
        } catch (Exception e) {
            log.error("Failed to sync calendar on WhatsApp confirm for appointment id {}: {}", appointment.getId(), e.getMessage());
        }

        String patientName = (patient != null && patient.getFirstName() != null) ? patient.getFirstName() : "there";
        String date = appointment.getAppointmentStart().format(DATE_FORMATTER);
        String time = appointment.getAppointmentStart().format(TIME_FORMATTER);
        String doctorName = getDoctorFullName(appointment.getDoctor());

        log.info("Appointment ID {} CONFIRMED by patient via WhatsApp", appointment.getId());

        return String.format(
                "Thank you %s! 🎉\nYour appointment for %s at %s with %s is confirmed. We look forward to seeing you!",
                patientName, date, time, doctorName);
    }

    private String handleReschedule(Appointment appointment, Patient patient) {
        if (appointment == null) {
            return String.format(
                    "Thank you for contacting %s! To reschedule or book a new appointment, please call our reception at %s.",
                    clinicName, clinicPhone);
        }

        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        appointmentRepository.save(appointment);

        try {
            calendarService.updateAppointmentEvent(appointment);
        } catch (Exception e) {
            log.error("Failed to sync calendar on WhatsApp reschedule for appointment id {}: {}", appointment.getId(), e.getMessage());
        }

        log.info("Appointment ID {} flagged as RESCHEDULED by patient via WhatsApp", appointment.getId());

        return String.format(
                "We have noted that you wish to reschedule your appointment. 🗓️\n" +
                "Our clinic team will reach out to you shortly, or feel free to call our reception at %s to choose a new date and time.",
                clinicPhone);
    }

    private String handleCancel(Appointment appointment, Patient patient) {
        if (appointment == null) {
            return String.format(
                    "No active upcoming appointment found to cancel. If you need any assistance, please call %s at %s.",
                    clinicName, clinicPhone);
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setConfirmationStatus(ConfirmationStatus.DECLINED);
        appointmentRepository.save(appointment);

        try {
            calendarService.cancelAppointmentEvent(appointment, "Cancelled by patient via WhatsApp");
        } catch (Exception e) {
            log.error("Failed to sync calendar on WhatsApp cancel for appointment id {}: {}", appointment.getId(), e.getMessage());
        }

        log.info("Appointment ID {} CANCELLED by patient via WhatsApp", appointment.getId());

        String date = appointment.getAppointmentStart().format(DATE_FORMATTER);
        return String.format(
                "Your appointment on %s has been cancelled. ❌\n" +
                "If you would like to book an appointment in the future, please call %s at %s. Have a wonderful day!",
                date, clinicName, clinicPhone);
    }

    private String handleOptOut(Patient patient, boolean optOut) {
        if (patient != null) {
            patient.setWhatsappOptOut(optOut);
            patientRepository.save(patient);
            log.info("Patient ID {} whatsappOptOut set to {} via WhatsApp", patient.getId(), optOut);
        }

        if (optOut) {
            return String.format(
                    "You have been unsubscribed from automated appointment reminders from %s. " +
                    "You will no longer receive WhatsApp reminders. (Reply START at any time to re-enable).",
                    clinicName);
        } else {
            return String.format(
                    "Welcome back! You have re-subscribed to automated appointment reminders from %s.",
                    clinicName);
        }
    }

    private String handleUnrecognizedInput() {
        return String.format(
                "Thank you for your message. Please reply with one of the following numbers:\n" +
                "1️⃣ Yes, I am available\n" +
                "2️⃣ Reschedule appointment\n" +
                "3️⃣ Cancel\n" +
                "4️⃣ Don't send these messages\n\n" +
                "Or call %s directly at %s.",
                clinicName, clinicPhone);
    }

    private boolean isConfirmChoice(String text) {
        return text.equals("1") || text.equals("yes") || text.equals("y")
                || text.equals("available") || text.equals("confirm")
                || text.equals("attending") || text.contains("i am available");
    }

    private boolean isRescheduleChoice(String text) {
        return text.equals("2") || text.equals("reschedule")
                || text.equals("reschedule appointment") || text.contains("reschedule")
                || text.equals("change");
    }

    private boolean isCancelChoice(String text) {
        return text.equals("3") || text.equals("cancel")
                || text.equals("cancel appointment") || text.equals("no")
                || text.contains("cancel");
    }

    private boolean isOptOutChoice(String text) {
        return text.equals("4") || text.equals("stop")
                || text.equals("unsubscribe") || text.contains("don't send")
                || text.contains("dont send") || text.equals("dnd");
    }

    private boolean isOptInChoice(String text) {
        return text.equals("start") || text.equals("unstop") || text.equals("subscribe");
    }

    private Appointment findUpcomingAppointment(String phone) {
        List<Appointment> appointments = appointmentRepository.findUpcomingByPatientPhone(phone);
        if (!appointments.isEmpty()) {
            return appointments.get(0);
        }

        // Try alternating formats (with / without +1)
        if (phone.startsWith("+1") && phone.length() == 12) {
            appointments = appointmentRepository.findUpcomingByPatientPhone(phone.substring(2));
            if (!appointments.isEmpty()) return appointments.get(0);
        } else if (phone.length() == 10) {
            appointments = appointmentRepository.findUpcomingByPatientPhone("+1" + phone);
            if (!appointments.isEmpty()) return appointments.get(0);
        }

        return null;
    }

    private Patient findPatient(String phone) {
        Optional<Patient> opt = patientRepository.findByPhoneNumberAndIsDeletedFalse(phone);
        if (opt.isPresent()) return opt.get();

        if (phone.startsWith("+1") && phone.length() == 12) {
            opt = patientRepository.findByPhoneNumberAndIsDeletedFalse(phone.substring(2));
            if (opt.isPresent()) return opt.get();
        } else if (phone.length() == 10) {
            opt = patientRepository.findByPhoneNumberAndIsDeletedFalse("+1" + phone);
            if (opt.isPresent()) return opt.get();
        }

        return null;
    }

    private String cleanPhoneNumber(String phone) {
        String cleaned = phone.trim();
        if (cleaned.startsWith("whatsapp:")) {
            cleaned = cleaned.substring("whatsapp:".length()).trim();
        }
        return cleaned;
    }

    private String getDoctorFullName(Doctor doctor) {
        if (doctor == null) return "the doctor";
        String first = (doctor.getFirstName() != null) ? doctor.getFirstName() : "";
        String last = (doctor.getLastName() != null) ? " " + doctor.getLastName() : "";
        return ("Dr. " + first + last).trim();
    }
}
