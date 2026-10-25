package com.dental.manage.service;

import com.dental.manage.config.CalendarProperties;
import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.ClinicDetails;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.Patient;
import com.dental.manage.entity.Service;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.CalendarProviderType;
import com.dental.manage.repository.ClinicDetailsRepository;
import com.dental.manage.service.impl.GoogleCalendarServiceImpl;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarServiceImplTest {

    @Mock
    private ClinicDetailsRepository clinicDetailsRepository;

    @Mock
    private Calendar calendarClient;

    @Mock
    private Calendar.Events eventsResource;

    @Mock
    private Calendar.Events.Insert insertRequest;

    @Mock
    private Calendar.Events.Get getRequest;

    @Mock
    private Calendar.Events.Update updateRequest;

    @Mock
    private Calendar.Events.Delete deleteRequest;

    private CalendarProperties calendarProperties;
    private GoogleCalendarServiceImpl googleCalendarService;

    private Doctor sampleDoctor;
    private Patient samplePatient;
    private Service sampleService;
    private Appointment sampleAppointment;
    private ClinicDetails sampleClinic;

    @BeforeEach
    void setUp() {
        calendarProperties = new CalendarProperties();
        calendarProperties.setProvider(CalendarProviderType.GOOGLE);
        calendarProperties.getGoogle().setEnabled(true);
        calendarProperties.getGoogle().setCalendarId("primary");
        calendarProperties.getGoogle().setSendUpdates("all");
        calendarProperties.getGoogle().setDefaultTimezone("Asia/Kolkata");

        sampleClinic = ClinicDetails.builder()
                .clinicId(1L)
                .clinicName("Smile Dental Care")
                .clinicPhone("+1-800-555-0199")
                .timezone("Asia/Kolkata")
                .build();
        sampleClinic.setId(1L);

        sampleDoctor = Doctor.builder()
                .clinicId(1L)
                .firstName("Sarah")
                .lastName("Smith")
                .email("dr.smith@dentalcare.com")
                .specialization("Endodontist")
                .build();
        sampleDoctor.setId(20L);

        samplePatient = Patient.builder()
                .clinicId(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@gmail.com")
                .phoneNumber("+1-555-0144")
                .build();
        samplePatient.setId(10L);

        sampleService = Service.builder()
                .name("Root Canal Treatment")
                .durationMinutes(60)
                .build();
        sampleService.setId(5L);

        sampleAppointment = Appointment.builder()
                .clinicId(1L)
                .doctor(sampleDoctor)
                .patient(samplePatient)
                .service(sampleService)
                .appointmentStart(LocalDateTime.of(2026, 11, 10, 10, 0))
                .appointmentEnd(LocalDateTime.of(2026, 11, 10, 11, 0))
                .reason("Severe tooth ache")
                .status(AppointmentStatus.SCHEDULED)
                .build();
        sampleAppointment.setId(101L);

        // Inject mock Calendar client
        googleCalendarService = new GoogleCalendarServiceImpl(calendarProperties, clinicDetailsRepository, calendarClient);
    }

    @Nested
    @DisplayName("Initialization & Enabled Status")
    class InitializationTests {

        @Test
        @DisplayName("Should be enabled when configured with Calendar client")
        void shouldBeEnabledWhenClientProvided() {
            assertThat(googleCalendarService.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should be disabled and not fail when credentials are missing")
        void shouldBeDisabledGracefullyWhenUnconfigured() {
            CalendarProperties props = new CalendarProperties();
            props.setProvider(CalendarProviderType.GOOGLE);
            props.getGoogle().setEnabled(true);

            GoogleCalendarServiceImpl unconfiguredService = new GoogleCalendarServiceImpl(props, clinicDetailsRepository);
            unconfiguredService.init();

            assertThat(unconfiguredService.isEnabled()).isFalse();
            assertThat(unconfiguredService.createAppointmentEvent(sampleAppointment)).isEmpty();
        }

        @Test
        @DisplayName("Should be disabled when provider is set to NONE")
        void shouldBeDisabledWhenProviderIsNone() {
            CalendarProperties props = new CalendarProperties();
            props.setProvider(CalendarProviderType.NONE);

            GoogleCalendarServiceImpl noneService = new GoogleCalendarServiceImpl(props, clinicDetailsRepository);
            noneService.init();

            assertThat(noneService.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Create Appointment Event")
    class CreateEventTests {

        @Test
        @DisplayName("Should create event with both doctor and patient as attendees and sendUpdates=all")
        void shouldCreateEventWithDoctorAndPatientAttendees() throws IOException {
            when(clinicDetailsRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(sampleClinic));
            when(calendarClient.events()).thenReturn(eventsResource);
            when(eventsResource.insert(eq("primary"), any(Event.class))).thenReturn(insertRequest);
            when(insertRequest.setSendUpdates("all")).thenReturn(insertRequest);

            Event mockCreated = new Event();
            mockCreated.setId("gcal_evt_12345");
            when(insertRequest.execute()).thenReturn(mockCreated);

            Optional<String> eventId = googleCalendarService.createAppointmentEvent(sampleAppointment);

            assertThat(eventId).isPresent().contains("gcal_evt_12345");

            // Verify event payload structure
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(eventsResource).insert(eq("primary"), eventCaptor.capture());
            Event captured = eventCaptor.getValue();

            assertThat(captured.getSummary()).isEqualTo("Dental Appointment: Root Canal Treatment - John Doe");
            assertThat(captured.getDescription()).contains("Doctor: Dr. Sarah Smith")
                    .contains("Patient: John Doe")
                    .contains("Service: Root Canal Treatment (60 mins)")
                    .contains("Patient Phone: +1-555-0144")
                    .contains("Patient Email: john.doe@gmail.com")
                    .contains("Reason / Notes: Severe tooth ache")
                    .contains("Status: SCHEDULED");

            // Attendees verification
            List<EventAttendee> attendees = captured.getAttendees();
            assertThat(attendees).hasSize(2);
            assertThat(attendees).extracting(EventAttendee::getEmail)
                    .containsExactlyInAnyOrder("dr.smith@dentalcare.com", "john.doe@gmail.com");

            // Reminders verification
            assertThat(captured.getReminders()).isNotNull();
            assertThat(captured.getReminders().getOverrides()).hasSize(3);

            // Date & Timezone verification
            assertThat(captured.getStart().getTimeZone()).isEqualTo("Asia/Kolkata");
            assertThat(captured.getEnd().getTimeZone()).isEqualTo("Asia/Kolkata");
            assertThat(captured.getStart().getDateTime().toStringRfc3339()).contains("2026-11-10T10:00:00");
        }

        @Test
        @DisplayName("Should create event when patient has no email (only doctor attendee)")
        void shouldCreateEventWhenPatientHasNoEmail() throws IOException {
            samplePatient.setEmail(null);

            when(clinicDetailsRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(sampleClinic));
            when(calendarClient.events()).thenReturn(eventsResource);
            when(eventsResource.insert(eq("primary"), any(Event.class))).thenReturn(insertRequest);
            when(insertRequest.setSendUpdates("all")).thenReturn(insertRequest);

            Event mockCreated = new Event();
            mockCreated.setId("gcal_evt_doctor_only");
            when(insertRequest.execute()).thenReturn(mockCreated);

            Optional<String> eventId = googleCalendarService.createAppointmentEvent(sampleAppointment);

            assertThat(eventId).isPresent().contains("gcal_evt_doctor_only");

            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(eventsResource).insert(eq("primary"), eventCaptor.capture());
            List<EventAttendee> attendees = eventCaptor.getValue().getAttendees();

            assertThat(attendees).hasSize(1);
            assertThat(attendees.get(0).getEmail()).isEqualTo("dr.smith@dentalcare.com");
        }

        @Test
        @DisplayName("Should return empty optional gracefully when Google API fails")
        void shouldHandleGoogleApiFailureGracefully() throws IOException {
            when(clinicDetailsRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(sampleClinic));
            when(calendarClient.events()).thenReturn(eventsResource);
            when(eventsResource.insert(eq("primary"), any(Event.class))).thenReturn(insertRequest);
            when(insertRequest.setSendUpdates("all")).thenReturn(insertRequest);
            when(insertRequest.execute()).thenThrow(new IOException("Google API network timeout"));

            Optional<String> eventId = googleCalendarService.createAppointmentEvent(sampleAppointment);

            assertThat(eventId).isEmpty();
        }

        @Test
        @DisplayName("Should gracefully fallback and create event without attendees when Google blocks service account attendees")
        void shouldFallbackWithoutAttendeesWhenForbiddenForServiceAccounts() throws IOException {
            when(clinicDetailsRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(sampleClinic));
            when(calendarClient.events()).thenReturn(eventsResource);
            when(eventsResource.insert(eq("primary"), any(Event.class))).thenReturn(insertRequest);
            when(insertRequest.setSendUpdates("all")).thenReturn(insertRequest);
            when(insertRequest.setSendUpdates("none")).thenReturn(insertRequest);

            HttpResponseException.Builder httpBuilder = new HttpResponseException.Builder(403, "Forbidden", new HttpHeaders());
            httpBuilder.setMessage("Service accounts cannot invite attendees without Domain-Wide Delegation of Authority.");
            GoogleJsonResponseException forbiddenForServiceAccounts = new GoogleJsonResponseException(httpBuilder, null);

            Event fallbackCreated = new Event();
            fallbackCreated.setId("fallback_event_123");

            // First execution throws forbiddenForServiceAccounts, retry execution succeeds
            when(insertRequest.execute())
                    .thenThrow(forbiddenForServiceAccounts)
                    .thenReturn(fallbackCreated);

            Optional<String> eventId = googleCalendarService.createAppointmentEvent(sampleAppointment);

            assertThat(eventId).isPresent().contains("fallback_event_123");
            verify(insertRequest).setSendUpdates("none");
        }
    }

    @Nested
    @DisplayName("Update Appointment Event")
    class UpdateEventTests {

        @Test
        @DisplayName("Should update existing event with new times and sendUpdates=all")
        void shouldUpdateExistingEventWithNewTimes() throws IOException {
            sampleAppointment.setCalendarEventId("existing_event_99");
            sampleAppointment.setAppointmentStart(LocalDateTime.of(2026, 11, 12, 14, 0));
            sampleAppointment.setAppointmentEnd(LocalDateTime.of(2026, 11, 12, 15, 0));
            sampleAppointment.setStatus(AppointmentStatus.RESCHEDULED);

            when(clinicDetailsRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(sampleClinic));
            when(calendarClient.events()).thenReturn(eventsResource);
            when(eventsResource.get("primary", "existing_event_99")).thenReturn(getRequest);

            Event existingEvent = new Event();
            existingEvent.setId("existing_event_99");
            when(getRequest.execute()).thenReturn(existingEvent);

            when(eventsResource.update(eq("primary"), eq("existing_event_99"), any(Event.class))).thenReturn(updateRequest);
            when(updateRequest.setSendUpdates("all")).thenReturn(updateRequest);
            when(updateRequest.execute()).thenReturn(existingEvent);

            boolean success = googleCalendarService.updateAppointmentEvent(sampleAppointment);

            assertThat(success).isTrue();
            verify(eventsResource).update(eq("primary"), eq("existing_event_99"), eq(existingEvent));
            verify(updateRequest).setSendUpdates("all");
            assertThat(existingEvent.getStart().getDateTime().toStringRfc3339()).contains("2026-11-12T14:00:00");
            assertThat(existingEvent.getDescription()).contains("Status: RESCHEDULED");
        }

        @Test
        @DisplayName("Should re-create event if Google Calendar returns 404 Not Found")
        void shouldRecreateEventIfNotFoundOnCalendar() throws IOException {
            sampleAppointment.setCalendarEventId("deleted_event_id");

            when(calendarClient.events()).thenReturn(eventsResource);
            when(eventsResource.get("primary", "deleted_event_id")).thenReturn(getRequest);

            HttpResponseException.Builder httpBuilder = new HttpResponseException.Builder(404, "Not Found", new HttpHeaders());
            GoogleJsonResponseException notFoundException = new GoogleJsonResponseException(httpBuilder, null);
            when(getRequest.execute()).thenThrow(notFoundException);

            // Re-creation mocks
            when(clinicDetailsRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(sampleClinic));
            when(eventsResource.insert(eq("primary"), any(Event.class))).thenReturn(insertRequest);
            when(insertRequest.setSendUpdates("all")).thenReturn(insertRequest);
            Event recreated = new Event();
            recreated.setId("new_recreated_id");
            when(insertRequest.execute()).thenReturn(recreated);

            boolean success = googleCalendarService.updateAppointmentEvent(sampleAppointment);

            assertThat(success).isTrue();
            assertThat(sampleAppointment.getCalendarEventId()).isEqualTo("new_recreated_id");
        }

        @Test
        @DisplayName("Should create event if appointment had no calendarEventId when update is requested")
        void shouldCreateEventIfNoPreviousCalendarId() throws IOException {
            sampleAppointment.setCalendarEventId(null);

            when(clinicDetailsRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(sampleClinic));
            when(calendarClient.events()).thenReturn(eventsResource);
            when(eventsResource.insert(eq("primary"), any(Event.class))).thenReturn(insertRequest);
            when(insertRequest.setSendUpdates("all")).thenReturn(insertRequest);
            Event created = new Event();
            created.setId("new_created_event_id");
            when(insertRequest.execute()).thenReturn(created);

            boolean success = googleCalendarService.updateAppointmentEvent(sampleAppointment);

            assertThat(success).isTrue();
            assertThat(sampleAppointment.getCalendarEventId()).isEqualTo("new_created_event_id");
        }
    }

    @Nested
    @DisplayName("Cancel Appointment Event")
    class CancelEventTests {

        @Test
        @DisplayName("Should delete/cancel event with sendUpdates=all")
        void shouldDeleteEventWithSendUpdates() throws IOException {
            sampleAppointment.setCalendarEventId("evt_to_cancel");

            when(calendarClient.events()).thenReturn(eventsResource);
            when(eventsResource.delete("primary", "evt_to_cancel")).thenReturn(deleteRequest);
            when(deleteRequest.setSendUpdates("all")).thenReturn(deleteRequest);

            boolean success = googleCalendarService.cancelAppointmentEvent(sampleAppointment, "Patient requested cancellation");

            assertThat(success).isTrue();
            verify(eventsResource).delete("primary", "evt_to_cancel");
            verify(deleteRequest).setSendUpdates("all");
            verify(deleteRequest).execute();
        }

        @Test
        @DisplayName("Should treat 404/410 on delete as successfully cancelled")
        void shouldTreat404AsSuccessfullyCancelled() throws IOException {
            sampleAppointment.setCalendarEventId("already_removed_evt");

            when(calendarClient.events()).thenReturn(eventsResource);
            when(eventsResource.delete("primary", "already_removed_evt")).thenReturn(deleteRequest);
            when(deleteRequest.setSendUpdates("all")).thenReturn(deleteRequest);

            HttpResponseException.Builder httpBuilder = new HttpResponseException.Builder(404, "Not Found", new HttpHeaders());
            GoogleJsonResponseException notFoundException = new GoogleJsonResponseException(httpBuilder, null);
            when(deleteRequest.execute()).thenThrow(notFoundException);

            boolean success = googleCalendarService.cancelAppointmentEvent(sampleAppointment, "Cancelled");

            assertThat(success).isTrue();
        }

        @Test
        @DisplayName("Should return false without calling Google if calendarEventId is null")
        void shouldReturnFalseIfNoCalendarEventId() {
            sampleAppointment.setCalendarEventId(null);

            boolean success = googleCalendarService.cancelAppointmentEvent(sampleAppointment, "Cancelled");

            assertThat(success).isFalse();
            verify(calendarClient, never()).events();
        }
    }
}
