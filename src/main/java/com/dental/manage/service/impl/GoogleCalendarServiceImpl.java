package com.dental.manage.service.impl;

import com.dental.manage.config.CalendarProperties;
import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.ClinicDetails;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.Patient;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.CalendarProviderType;
import com.dental.manage.repository.ClinicDetailsRepository;
import com.dental.manage.service.CalendarService;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Production-ready Google Calendar integration service.
 * Automatically synchronizes appointment creations, updates, and cancellations
 * onto patient and doctor Google Calendars via attendees and invite updates.
 */
@Service
@Slf4j
public class GoogleCalendarServiceImpl implements CalendarService {

    private final CalendarProperties calendarProperties;
    private final ClinicDetailsRepository clinicDetailsRepository;

    private Calendar calendarClient;
    private boolean configured = false;

    @Autowired
    public GoogleCalendarServiceImpl(
            CalendarProperties calendarProperties,
            ClinicDetailsRepository clinicDetailsRepository) {
        this.calendarProperties = calendarProperties;
        this.clinicDetailsRepository = clinicDetailsRepository;
    }

    /**
     * Testing constructor allowing injection of a mock or custom Calendar client.
     */
    public GoogleCalendarServiceImpl(
            CalendarProperties calendarProperties,
            ClinicDetailsRepository clinicDetailsRepository,
            Calendar calendarClient) {
        this.calendarProperties = calendarProperties;
        this.clinicDetailsRepository = clinicDetailsRepository;
        this.calendarClient = calendarClient;
        this.configured = calendarClient != null;
    }

    @PostConstruct
    public void init() {
        if (this.calendarClient != null) {
            this.configured = true;
            return;
        }

        if (calendarProperties.getProvider() != CalendarProviderType.GOOGLE
                || !calendarProperties.getGoogle().isEnabled()) {
            log.info("Google Calendar integration is disabled via configuration (provider={})",
                    calendarProperties.getProvider());
            return;
        }

        try {
            HttpRequestInitializer requestInitializer = createCredentialsInitializer();
            if (requestInitializer != null) {
                this.calendarClient = new Calendar.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        requestInitializer)
                        .setApplicationName(calendarProperties.getGoogle().getApplicationName())
                        .build();
                this.configured = true;
                log.info("Google Calendar client initialized successfully (calendarId={})",
                        calendarProperties.getGoogle().getCalendarId());
            } else {
                log.warn("Google Calendar credentials not configured — calendar synchronization is in standby mode.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Google Calendar client: {}", e.getMessage(), e);
            this.configured = false;
        }
    }

    private HttpRequestInitializer createCredentialsInitializer() {
        CalendarProperties.GoogleProperties googleProps = calendarProperties.getGoogle();

        // 1. Try file path
        if (googleProps.getCredentialsPath() != null && !googleProps.getCredentialsPath().isBlank()) {
            try (InputStream is = new FileInputStream(googleProps.getCredentialsPath().trim())) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(is)
                        .createScoped(Collections.singleton(CalendarScopes.CALENDAR));
                return new HttpCredentialsAdapter(credentials);
            } catch (Exception e) {
                log.error("Error loading Google credentials from file path '{}': {}",
                        googleProps.getCredentialsPath(), e.getMessage());
            }
        }

        // 2. Try raw JSON string
        if (googleProps.getCredentialsJson() != null && !googleProps.getCredentialsJson().isBlank()) {
            try (InputStream is = new ByteArrayInputStream(googleProps.getCredentialsJson().getBytes(StandardCharsets.UTF_8))) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(is)
                        .createScoped(Collections.singleton(CalendarScopes.CALENDAR));
                return new HttpCredentialsAdapter(credentials);
            } catch (Exception e) {
                log.error("Error loading Google credentials from JSON string: {}", e.getMessage());
            }
        }

        // 3. Try OAuth2 user credentials (client-id, client-secret, refresh-token)
        if (googleProps.getClientId() != null && !googleProps.getClientId().isBlank()
                && googleProps.getClientSecret() != null && !googleProps.getClientSecret().isBlank()
                && googleProps.getRefreshToken() != null && !googleProps.getRefreshToken().isBlank()) {
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(googleProps.getClientId().trim())
                    .setClientSecret(googleProps.getClientSecret().trim())
                    .setRefreshToken(googleProps.getRefreshToken().trim())
                    .build();
            return new HttpCredentialsAdapter(credentials);
        }

        // 4. Try Google Application Default Credentials (e.g. GOOGLE_APPLICATION_CREDENTIALS env var)
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));
            return new HttpCredentialsAdapter(credentials);
        } catch (Exception ignored) {
            // No default credentials found
        }

        return null;
    }

    @Override
    public Optional<String> createAppointmentEvent(Appointment appointment) {
        if (!isEnabled()) {
            log.debug("Google Calendar sync disabled, skipping event creation for appointment id: {}",
                    appointment != null ? appointment.getId() : null);
            return Optional.empty();
        }

        if (appointment == null || appointment.getAppointmentStart() == null || appointment.getAppointmentEnd() == null) {
            log.warn("Cannot create calendar event: missing appointment data");
            return Optional.empty();
        }

        try {
            Event event = buildCalendarEvent(appointment);
            String calendarId = resolveCalendarId(appointment.getClinicId());
            String sendUpdates = calendarProperties.getGoogle().getSendUpdates();

            log.info("Creating Google Calendar event for appointment id={}, calendarId={}, sendUpdates={}",
                    appointment.getId(), calendarId, sendUpdates);

            Event created;
            try {
                created = calendarClient.events().insert(calendarId, event)
                        .setSendUpdates(sendUpdates)
                        .execute();
            } catch (GoogleJsonResponseException gjre) {
                if (isForbiddenForServiceAccounts(gjre)) {
                    log.warn("Google API constraint: Service accounts without Domain-Wide Delegation cannot invite external attendees. Retrying event creation on calendar '{}' without attendee invitations.", calendarId);
                    event.setAttendees(null);
                    created = calendarClient.events().insert(calendarId, event)
                            .setSendUpdates("none")
                            .execute();
                } else {
                    throw gjre;
                }
            }

            String eventId = created.getId();
            log.info("Google Calendar event successfully created with id: {} for appointment id: {}",
                    eventId, appointment.getId());
            return Optional.ofNullable(eventId);
        } catch (Exception e) {
            log.error("Failed to create Google Calendar event for appointment id {}: {}",
                    appointment.getId(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public boolean updateAppointmentEvent(Appointment appointment) {
        if (!isEnabled()) {
            log.debug("Google Calendar sync disabled, skipping event update for appointment id: {}",
                    appointment != null ? appointment.getId() : null);
            return false;
        }

        if (appointment == null) {
            return false;
        }

        // If no calendar event ID exists yet, create one now
        if (appointment.getCalendarEventId() == null || appointment.getCalendarEventId().isBlank()) {
            Optional<String> newId = createAppointmentEvent(appointment);
            newId.ifPresent(appointment::setCalendarEventId);
            return newId.isPresent();
        }

        try {
            String calendarId = resolveCalendarId(appointment.getClinicId());
            String eventId = appointment.getCalendarEventId();
            String sendUpdates = calendarProperties.getGoogle().getSendUpdates();

            Event existing;
            try {
                existing = calendarClient.events().get(calendarId, eventId).execute();
            } catch (GoogleJsonResponseException gjre) {
                if (gjre.getStatusCode() == 404) {
                    log.warn("Google Calendar event id {} not found on calendar {}. Re-creating event.",
                            eventId, calendarId);
                    Optional<String> recreatedId = createAppointmentEvent(appointment);
                    recreatedId.ifPresent(appointment::setCalendarEventId);
                    return recreatedId.isPresent();
                }
                throw gjre;
            }

            // Update details on the existing event
            populateEventDetails(existing, appointment);

            try {
                calendarClient.events().update(calendarId, eventId, existing)
                        .setSendUpdates(sendUpdates)
                        .execute();
            } catch (GoogleJsonResponseException gjre) {
                if (isForbiddenForServiceAccounts(gjre)) {
                    log.warn("Google API constraint: Service accounts without Domain-Wide Delegation cannot invite external attendees. Retrying event update on calendar '{}' without attendee invitations.", calendarId);
                    existing.setAttendees(null);
                    calendarClient.events().update(calendarId, eventId, existing)
                            .setSendUpdates("none")
                            .execute();
                } else {
                    throw gjre;
                }
            }

            log.info("Google Calendar event id {} updated successfully for appointment id: {}",
                    eventId, appointment.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to update Google Calendar event for appointment id {}: {}",
                    appointment.getId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean cancelAppointmentEvent(Appointment appointment, String reason) {
        if (!isEnabled()) {
            return false;
        }

        if (appointment == null || appointment.getCalendarEventId() == null || appointment.getCalendarEventId().isBlank()) {
            return false;
        }

        try {
            String calendarId = resolveCalendarId(appointment.getClinicId());
            String eventId = appointment.getCalendarEventId();
            String sendUpdates = calendarProperties.getGoogle().getSendUpdates();

            log.info("Cancelling Google Calendar event id: {} for appointment id: {}", eventId, appointment.getId());

            try {
                calendarClient.events().delete(calendarId, eventId)
                        .setSendUpdates(sendUpdates)
                        .execute();
            } catch (GoogleJsonResponseException gjre) {
                if (isForbiddenForServiceAccounts(gjre)) {
                    calendarClient.events().delete(calendarId, eventId)
                            .setSendUpdates("none")
                            .execute();
                } else {
                    throw gjre;
                }
            }

            log.info("Google Calendar event id: {} cancelled successfully", eventId);
            return true;
        } catch (GoogleJsonResponseException gjre) {
            if (gjre.getStatusCode() == 404 || gjre.getStatusCode() == 410) {
                log.info("Google Calendar event id: {} was already removed/cancelled", appointment.getCalendarEventId());
                return true;
            }
            log.error("Google API error cancelling event id {}: {}", appointment.getCalendarEventId(), gjre.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Failed to cancel Google Calendar event for appointment id {}: {}",
                    appointment.getId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Optional<String> syncAppointmentEvent(Appointment appointment) {
        if (appointment == null) {
            return Optional.empty();
        }
        if (appointment.getCalendarEventId() != null && !appointment.getCalendarEventId().isBlank()) {
            boolean updated = updateAppointmentEvent(appointment);
            return updated ? Optional.of(appointment.getCalendarEventId()) : Optional.empty();
        }
        return createAppointmentEvent(appointment);
    }

    @Override
    public boolean isEnabled() {
        return configured && calendarClient != null;
    }

    private Event buildCalendarEvent(Appointment appointment) {
        Event event = new Event();
        populateEventDetails(event, appointment);
        return event;
    }

    private void populateEventDetails(Event event, Appointment appointment) {
        Doctor doctor = appointment.getDoctor();
        Patient patient = appointment.getPatient();
        String doctorFullName = getDoctorFullName(doctor);
        String patientFullName = getPatientFullName(patient);

        String serviceName = (appointment.getService() != null && appointment.getService().getName() != null)
                ? appointment.getService().getName()
                : "Dental Treatment";

        // Summary / Title
        String summary = String.format("Dental Appointment: %s - %s", serviceName, patientFullName);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            summary = "[CANCELLED] " + summary;
        }
        event.setSummary(summary);

        // Location
        String location = resolveLocation(appointment.getClinicId());
        if (location != null && !location.isBlank()) {
            event.setLocation(location);
        }

        // Detailed Description
        StringBuilder desc = new StringBuilder();
        desc.append("🦷 Dental Appointment Details\n");
        desc.append("─────────────────────────────\n");
        desc.append("Patient: ").append(patientFullName).append("\n");
        if (patient != null && patient.getPhoneNumber() != null && !patient.getPhoneNumber().isBlank()) {
            desc.append("Patient Phone: ").append(patient.getPhoneNumber()).append("\n");
        }
        if (patient != null && patient.getEmail() != null && !patient.getEmail().isBlank()) {
            desc.append("Patient Email: ").append(patient.getEmail()).append("\n");
        }
        desc.append("Doctor: Dr. ").append(doctorFullName).append("\n");
        if (doctor != null && doctor.getSpecialization() != null && !doctor.getSpecialization().isBlank()) {
            desc.append("Specialization: ").append(doctor.getSpecialization()).append("\n");
        }
        desc.append("Service: ").append(serviceName);
        if (appointment.getService() != null && appointment.getService().getDurationMinutes() != null) {
            desc.append(" (").append(appointment.getService().getDurationMinutes()).append(" mins)");
        }
        desc.append("\n");
        desc.append("Status: ").append(appointment.getStatus() != null ? appointment.getStatus().name() : "SCHEDULED").append("\n");

        if (appointment.getReason() != null && !appointment.getReason().isBlank()) {
            desc.append("Reason / Notes: ").append(appointment.getReason().trim()).append("\n");
        }
        if (appointment.getId() != null) {
            desc.append("Booking Reference: #").append(appointment.getId()).append("\n");
        }
        event.setDescription(desc.toString());

        // Timezone & Start/End
        ZoneId zoneId = resolveTimezone(appointment.getClinicId());
        EventDateTime start = toEventDateTime(appointment.getAppointmentStart(), zoneId);
        EventDateTime end = toEventDateTime(appointment.getAppointmentEnd(), zoneId);
        event.setStart(start);
        event.setEnd(end);

        // Attendees: Doctor & Patient
        List<EventAttendee> attendees = new ArrayList<>();
        if (doctor != null && doctor.getEmail() != null && !doctor.getEmail().isBlank()) {
            EventAttendee doctorAttendee = new EventAttendee();
            doctorAttendee.setEmail(doctor.getEmail().trim());
            doctorAttendee.setDisplayName("Dr. " + doctorFullName);
            attendees.add(doctorAttendee);
        }

        if (patient != null && patient.getEmail() != null && !patient.getEmail().isBlank()) {
            EventAttendee patientAttendee = new EventAttendee();
            patientAttendee.setEmail(patient.getEmail().trim());
            patientAttendee.setDisplayName(patientFullName);
            attendees.add(patientAttendee);
        }
        event.setAttendees(attendees);

        // Reminders: 24h & 1h popups, 24h email
        Event.Reminders reminders = new Event.Reminders();
        reminders.setUseDefault(false);
        List<EventReminder> reminderList = List.of(
                new EventReminder().setMethod("popup").setMinutes(24 * 60),
                new EventReminder().setMethod("popup").setMinutes(60),
                new EventReminder().setMethod("email").setMinutes(24 * 60)
        );
        reminders.setOverrides(reminderList);
        event.setReminders(reminders);
    }

    private EventDateTime toEventDateTime(LocalDateTime localDateTime, ZoneId zoneId) {
        if (localDateTime == null) return null;
        ZonedDateTime zdt = localDateTime.atZone(zoneId);
        String rfc3339 = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        DateTime googleDateTime = new DateTime(rfc3339);
        return new EventDateTime()
                .setDateTime(googleDateTime)
                .setTimeZone(zoneId.getId());
    }

    private String getDoctorFullName(Doctor doctor) {
        if (doctor == null) return "Assigned Doctor";
        String first = doctor.getFirstName() != null ? doctor.getFirstName().trim() : "";
        String last = doctor.getLastName() != null ? doctor.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "Doctor" : full;
    }

    private String getPatientFullName(Patient patient) {
        if (patient == null) return "Patient";
        String first = patient.getFirstName() != null ? patient.getFirstName().trim() : "";
        String last = patient.getLastName() != null ? patient.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "Patient" : full;
    }

    private ZoneId resolveTimezone(Long clinicId) {
        String tzName = null;
        if (clinicId != null) {
            try {
                Optional<ClinicDetails> clinic = clinicDetailsRepository.findByIdAndIsDeletedFalse(clinicId);
                if (clinic.isPresent() && clinic.get().getTimezone() != null && !clinic.get().getTimezone().isBlank()) {
                    tzName = clinic.get().getTimezone().trim();
                }
            } catch (Exception ignored) {
            }
        }
        if (tzName == null || tzName.isBlank()) {
            tzName = calendarProperties.getGoogle().getDefaultTimezone();
        }
        if (tzName == null || tzName.isBlank()) {
            tzName = "Asia/Kolkata";
        }
        try {
            return ZoneId.of(tzName);
        } catch (Exception e) {
            log.warn("Invalid timezone '{}', defaulting to system default", tzName);
            return ZoneId.systemDefault();
        }
    }

    private String resolveLocation(Long clinicId) {
        if (clinicId != null) {
            try {
                Optional<ClinicDetails> clinic = clinicDetailsRepository.findByIdAndIsDeletedFalse(clinicId);
                if (clinic.isPresent()) {
                    ClinicDetails details = clinic.get();
                    String name = details.getClinicName() != null ? details.getClinicName() : "Dental Clinic";
                    String phone = details.getClinicPhone() != null ? details.getClinicPhone() : "";
                    return phone.isEmpty() ? name : name + " (Tel: " + phone + ")";
                }
            } catch (Exception ignored) {
            }
        }
        return "Dental Clinic";
    }

    private String resolveCalendarId(Long clinicId) {
        String calendarId = calendarProperties.getGoogle().getCalendarId();
        return (calendarId != null && !calendarId.isBlank()) ? calendarId.trim() : "primary";
    }

    private boolean isForbiddenForServiceAccounts(GoogleJsonResponseException e) {
        if (e.getStatusCode() == 403) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("forbiddenForServiceAccounts") || msg.contains("Service accounts cannot invite attendees")) {
                return true;
            }
            if (e.getDetails() != null && e.getDetails().getErrors() != null) {
                return e.getDetails().getErrors().stream()
                        .anyMatch(err -> "forbiddenForServiceAccounts".equalsIgnoreCase(err.getReason())
                                || (err.getMessage() != null && err.getMessage().contains("Service accounts cannot invite attendees")));
            }
        }
        return false;
    }
}
