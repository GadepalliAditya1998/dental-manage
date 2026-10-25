package com.dental.manage.service;

import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.Patient;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.ConfirmationStatus;
import com.dental.manage.repository.AppointmentRepository;
import com.dental.manage.repository.PatientRepository;
import com.dental.manage.service.CalendarService;
import com.dental.manage.service.impl.WhatsAppResponseServiceImpl;
import com.dental.manage.service.provider.WhatsAppProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppResponseServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private WhatsAppProvider whatsAppProvider;

    @Mock
    private CalendarService calendarService;

    @InjectMocks
    private WhatsAppResponseServiceImpl responseService;

    private Patient samplePatient;
    private Doctor sampleDoctor;
    private Appointment sampleAppointment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(responseService, "clinicName", "Smile Dental Clinic");
        ReflectionTestUtils.setField(responseService, "clinicPhone", "+1-800-555-0199");

        samplePatient = Patient.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .phoneNumber("+15551234567")
                .whatsappOptOut(false)
                .build();
        samplePatient.setId(10L);

        sampleDoctor = Doctor.builder()
                .firstName("Emily")
                .lastName("Davis")
                .build();
        sampleDoctor.setId(20L);

        sampleAppointment = Appointment.builder()
                .patient(samplePatient)
                .doctor(sampleDoctor)
                .appointmentStart(LocalDateTime.now().plusDays(1).withHour(14).withMinute(0))
                .status(AppointmentStatus.REMINDER_SENT)
                .confirmationStatus(ConfirmationStatus.PENDING)
                .build();
        sampleAppointment.setId(30L);
    }

    @Test
    @DisplayName("Option 1 ('1' or 'yes') confirms appointment and sets confirmation status")
    void shouldConfirmAppointmentOnOption1() {
        when(appointmentRepository.findUpcomingByPatientPhone("+15551234567"))
                .thenReturn(List.of(sampleAppointment));

        String reply = responseService.processInboundMessage("whatsapp:+15551234567", "1");

        assertThat(reply).contains("confirmed");
        assertThat(sampleAppointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(sampleAppointment.getConfirmationStatus()).isEqualTo(ConfirmationStatus.CONFIRMED);
        verify(appointmentRepository).save(sampleAppointment);
        verify(calendarService).updateAppointmentEvent(sampleAppointment);
        verify(whatsAppProvider).sendMessage(eq("whatsapp:+15551234567"), anyString());
    }

    @Test
    @DisplayName("Option 2 ('2' or 'reschedule') sets appointment status to RESCHEDULED")
    void shouldFlagRescheduleOnOption2() {
        when(appointmentRepository.findUpcomingByPatientPhone("+15551234567"))
                .thenReturn(List.of(sampleAppointment));

        String reply = responseService.processInboundMessage("whatsapp:+15551234567", "2");

        assertThat(reply).contains("reschedule");
        assertThat(sampleAppointment.getStatus()).isEqualTo(AppointmentStatus.RESCHEDULED);
        verify(appointmentRepository).save(sampleAppointment);
        verify(calendarService).updateAppointmentEvent(sampleAppointment);
        verify(whatsAppProvider).sendMessage(eq("whatsapp:+15551234567"), anyString());
    }

    @Test
    @DisplayName("Option 3 ('3' or 'cancel') cancels appointment and sets confirmation status to DECLINED")
    void shouldCancelAppointmentOnOption3() {
        when(appointmentRepository.findUpcomingByPatientPhone("+15551234567"))
                .thenReturn(List.of(sampleAppointment));

        String reply = responseService.processInboundMessage("whatsapp:+15551234567", "3");

        assertThat(reply).contains("cancelled");
        assertThat(sampleAppointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(sampleAppointment.getConfirmationStatus()).isEqualTo(ConfirmationStatus.DECLINED);
        verify(appointmentRepository).save(sampleAppointment);
        verify(calendarService).cancelAppointmentEvent(eq(sampleAppointment), anyString());
        verify(whatsAppProvider).sendMessage(eq("whatsapp:+15551234567"), anyString());
    }

    @Test
    @DisplayName("Option 4 ('4' or 'stop' or 'don't send') marks patient as opted out")
    void shouldOptOutPatientOnOption4() {
        when(appointmentRepository.findUpcomingByPatientPhone("+15551234567"))
                .thenReturn(List.of(sampleAppointment));

        String reply = responseService.processInboundMessage("whatsapp:+15551234567", "4");

        assertThat(reply).contains("unsubscribed");
        assertThat(samplePatient.getWhatsappOptOut()).isTrue();
        verify(patientRepository).save(samplePatient);
        verify(whatsAppProvider).sendMessage(eq("whatsapp:+15551234567"), anyString());
    }

    @Test
    @DisplayName("'start' or 'unstop' re-enables reminders by clearing opt-out flag")
    void shouldReSubscribePatientOnStart() {
        samplePatient.setWhatsappOptOut(true);
        when(appointmentRepository.findUpcomingByPatientPhone("+15551234567"))
                .thenReturn(Collections.emptyList());
        when(patientRepository.findByPhoneNumberAndIsDeletedFalse("+15551234567"))
                .thenReturn(Optional.of(samplePatient));

        String reply = responseService.processInboundMessage("whatsapp:+15551234567", "START");

        assertThat(reply).contains("re-subscribed");
        assertThat(samplePatient.getWhatsappOptOut()).isFalse();
        verify(patientRepository).save(samplePatient);
    }

    @Test
    @DisplayName("Unrecognized input returns interactive menu options guide")
    void shouldReturnGuideOnUnrecognizedInput() {
        when(appointmentRepository.findUpcomingByPatientPhone("+15551234567"))
                .thenReturn(List.of(sampleAppointment));

        String reply = responseService.processInboundMessage("whatsapp:+15551234567", "hello what is this");

        assertThat(reply).contains("1️⃣ Yes, I am available");
        assertThat(reply).contains("2️⃣ Reschedule appointment");
        assertThat(reply).contains("3️⃣ Cancel");
        assertThat(reply).contains("4️⃣ Don't send these messages");
        verify(whatsAppProvider).sendMessage(eq("whatsapp:+15551234567"), anyString());
    }
}
