package com.dental.manage.service;

import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.Patient;
import com.dental.manage.entity.Service;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.repository.AppointmentRepository;
import com.dental.manage.service.impl.AppointmentReminderServiceImpl;
import com.dental.manage.service.provider.WhatsAppProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private WhatsAppProvider whatsAppProvider;

    @InjectMocks
    private AppointmentReminderServiceImpl reminderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reminderService, "clinicTimezone", "Asia/Kolkata");
        ReflectionTestUtils.setField(reminderService, "daysBefore", 1);
    }

    @Test
    @DisplayName("sendUpcomingAppointmentReminders dispatches reminders and updates status to REMINDER_SENT")
    void shouldSendRemindersSuccessfully() {
        Patient patient = Patient.builder()
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+15551234567")
                .whatsappOptOut(false)
                .build();
        patient.setId(101L);

        Doctor doctor = Doctor.builder()
                .firstName("Sarah")
                .lastName("Smith")
                .build();
        doctor.setId(201L);

        Service service = Service.builder()
                .name("Teeth Whitening")
                .build();
        service.setId(301L);

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .service(service)
                .appointmentStart(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                .status(AppointmentStatus.SCHEDULED)
                .build();
        appointment.setId(1L);

        when(appointmentRepository.findAppointmentsNeedingReminderTomorrow(
                eq(AppointmentStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(appointment));

        when(whatsAppProvider.sendAppointmentReminder(appointment)).thenReturn(true);

        int sent = reminderService.sendUpcomingAppointmentReminders();

        assertThat(sent).isEqualTo(1);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.REMINDER_SENT);
        assertThat(appointment.getReminderSentAt()).isNotNull();
        verify(appointmentRepository, times(1)).save(appointment);
    }

    @Test
    @DisplayName("sendUpcomingAppointmentReminders does not update status if provider fails to send")
    void shouldNotUpdateStatusWhenProviderFails() {
        Patient patient = Patient.builder().phoneNumber("+15551234567").build();
        Appointment appointment = Appointment.builder()
                .patient(patient)
                .appointmentStart(LocalDateTime.now().plusDays(1))
                .status(AppointmentStatus.SCHEDULED)
                .build();
        appointment.setId(2L);

        when(appointmentRepository.findAppointmentsNeedingReminderTomorrow(
                eq(AppointmentStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(appointment));

        when(whatsAppProvider.sendAppointmentReminder(appointment)).thenReturn(false);

        int sent = reminderService.sendUpcomingAppointmentReminders();

        assertThat(sent).isEqualTo(0);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(appointment.getReminderSentAt()).isNull();
        verify(appointmentRepository, never()).save(appointment);
    }

    @Test
    @DisplayName("sendUpcomingAppointmentReminders returns 0 when no appointments need reminders")
    void shouldReturnZeroWhenNoAppointmentsFound() {
        when(appointmentRepository.findAppointmentsNeedingReminderTomorrow(
                eq(AppointmentStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        int sent = reminderService.sendUpcomingAppointmentReminders();

        assertThat(sent).isEqualTo(0);
        verify(whatsAppProvider, never()).sendAppointmentReminder(any());
    }

    @Test
    @DisplayName("sendRemindersForDate handles specific date correctly")
    void shouldSendRemindersForSpecificDate() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        when(appointmentRepository.findAppointmentsNeedingReminderTomorrow(
                eq(AppointmentStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        int sent = reminderService.sendRemindersForDate(date);

        assertThat(sent).isEqualTo(0);
        verify(appointmentRepository).findAppointmentsNeedingReminderTomorrow(
                eq(AppointmentStatus.SCHEDULED), eq(date.atStartOfDay()), any(LocalDateTime.class));
    }
}
