package com.dental.manage.service;

import com.dental.manage.dto.request.AppointmentRequest;
import com.dental.manage.dto.request.CalendarEventRequest;
import com.dental.manage.dto.response.AppointmentResponse;
import com.dental.manage.dto.response.DoctorResponse;
import com.dental.manage.dto.response.PatientResponse;
import com.dental.manage.dto.response.ServiceResponse;
import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.DoctorAvailability;
import com.dental.manage.entity.Patient;
import com.dental.manage.entity.Service;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.BookingSource;
import com.dental.manage.enums.ConfirmationStatus;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.mapper.AppointmentMapper;
import com.dental.manage.repository.AppointmentRepository;
import com.dental.manage.repository.CallRecordRepository;
import com.dental.manage.repository.ClinicDetailsRepository;
import com.dental.manage.repository.DoctorAvailabilityRepository;
import com.dental.manage.repository.DoctorRepository;
import com.dental.manage.repository.PatientRepository;
import com.dental.manage.repository.ServiceRepository;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.service.CalendarService;
import com.dental.manage.service.DoctorManagementService;
import com.dental.manage.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private CallRecordRepository callRecordRepository;

    @Mock
    private DoctorAvailabilityRepository doctorAvailabilityRepository;

    @Mock
    private ClinicDetailsRepository clinicDetailsRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private CalendarService calendarService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorManagementService doctorManagementService;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private Patient patient;
    private Doctor doctor;
    private Service service;
    private Appointment appointment;
    private AppointmentResponse appointmentResponse;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        startTime = LocalDateTime.of(2026, 10, 25, 10, 0, 0);
        endTime = LocalDateTime.of(2026, 10, 25, 10, 45, 0);

        patient = Patient.builder()
                .clinicId(1L)
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1-555-0100")
                .build();
        patient.setId(10L);

        doctor = Doctor.builder()
                .clinicId(1L)
                .firstName("Sarah")
                .lastName("Smith")
                .isActive(true)
                .build();
        doctor.setId(20L);

        service = Service.builder()
                .clinicId(1L)
                .name("Root Canal")
                .durationMinutes(45)
                .price(new BigDecimal("250.00"))
                .isActive(true)
                .build();
        service.setId(30L);

        appointment = Appointment.builder()
                .clinicId(1L)
                .patient(patient)
                .doctor(doctor)
                .service(service)
                .appointmentStart(startTime)
                .appointmentEnd(endTime)
                .reason("Severe toothache")
                .status(AppointmentStatus.SCHEDULED)
                .confirmationStatus(ConfirmationStatus.PENDING)
                .build();
        appointment.setId(100L);

        appointmentResponse = AppointmentResponse.builder()
                .id(100L)
                .clinicId(1L)
                .patient(PatientResponse.builder().id(10L).firstName("John").lastName("Doe").build())
                .doctor(DoctorResponse.builder().id(20L).firstName("Sarah").lastName("Smith").build())
                .service(ServiceResponse.builder().id(30L).name("Root Canal").durationMinutes(45).build())
                .appointmentStart(startTime)
                .appointmentEnd(endTime)
                .reason("Severe toothache")
                .status(AppointmentStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("bookAppointment creates appointment successfully with reason, patientId, doctorId, serviceId, start/end time")
    void bookAppointment_success() {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(10L)
                .doctorId(20L)
                .serviceId(30L)
                .startTime(startTime)
                .endTime(endTime)
                .reason("Severe toothache")
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(service));
        when(appointmentRepository.findOverlappingDoctorAppointments(20L, startTime, endTime)).thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toResponse(appointment)).thenReturn(appointmentResponse);

        AppointmentResponse result = appointmentService.bookAppointment(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getReason()).isEqualTo("Severe toothache");
        assertThat(result.getAppointmentStart()).isEqualTo(startTime);
        assertThat(result.getAppointmentEnd()).isEqualTo(endTime);

        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("bookAppointment calculates endTime automatically from service duration when endTime is null")
    void bookAppointment_autoCalculatesEndTime() {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(10L)
                .doctorId(20L)
                .serviceId(30L)
                .startTime(startTime)
                .endTime(null) // omitted
                .reason("Routine checkup")
                .build();

        LocalDateTime expectedEnd = startTime.plusMinutes(45);

        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(service));
        when(appointmentRepository.findOverlappingDoctorAppointments(20L, startTime, expectedEnd)).thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toResponse(appointment)).thenReturn(appointmentResponse);

        AppointmentResponse result = appointmentService.bookAppointment(request);

        assertThat(result).isNotNull();
        verify(appointmentRepository).findOverlappingDoctorAppointments(20L, startTime, expectedEnd);
    }

    @Test
    @DisplayName("bookAppointment throws ResourceNotFoundException when patient not found")
    void bookAppointment_patientNotFound() {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(999L)
                .doctorId(20L)
                .serviceId(30L)
                .startTime(startTime)
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.bookAppointment(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found with id: 999");
    }

    @Test
    @DisplayName("bookAppointment throws ResourceNotFoundException when doctor not found")
    void bookAppointment_doctorNotFound() {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(10L)
                .doctorId(999L)
                .serviceId(30L)
                .startTime(startTime)
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.bookAppointment(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Doctor not found with id: 999");
    }

    @Test
    @DisplayName("bookAppointment throws IllegalArgumentException when doctor is inactive")
    void bookAppointment_doctorInactive() {
        doctor.setIsActive(false);
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(10L)
                .doctorId(20L)
                .serviceId(30L)
                .startTime(startTime)
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> appointmentService.bookAppointment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("bookAppointment throws ResourceNotFoundException when service not found")
    void bookAppointment_serviceNotFound() {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(10L)
                .doctorId(20L)
                .serviceId(999L)
                .startTime(startTime)
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.bookAppointment(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service not found with id: 999");
    }

    @Test
    @DisplayName("bookAppointment throws IllegalArgumentException when end time is before start time")
    void bookAppointment_invalidEndTime() {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(10L)
                .doctorId(20L)
                .serviceId(30L)
                .startTime(startTime)
                .endTime(startTime.minusMinutes(30))
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(service));

        assertThatThrownBy(() -> appointmentService.bookAppointment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be after start time");
    }

    @Test
    @DisplayName("bookAppointment throws IllegalArgumentException when doctor has conflicting appointment")
    void bookAppointment_doctorConflict() {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(10L)
                .doctorId(20L)
                .serviceId(30L)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(service));
        when(appointmentRepository.findOverlappingDoctorAppointments(20L, startTime, endTime))
                .thenReturn(List.of(appointment));

        assertThatThrownBy(() -> appointmentService.bookAppointment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has an appointment scheduled");
    }

    @Test
    @DisplayName("getAppointmentById returns appointment when found")
    void getAppointmentById_success() {
        when(appointmentRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(appointment));
        when(appointmentMapper.toResponse(appointment)).thenReturn(appointmentResponse);

        AppointmentResponse result = appointmentService.getAppointmentById(100L, null);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("cancelAppointment sets status to CANCELLED and records reason")
    void cancelAppointment_success() {
        when(appointmentRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toResponse(appointment)).thenReturn(appointmentResponse);

        appointmentResponse.setStatus(AppointmentStatus.CANCELLED);

        AppointmentResponse result = appointmentService.cancelAppointment(100L, "Emergency reschedule", null);

        assertThat(result).isNotNull();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getReason()).contains("Emergency reschedule");
    }

    @Test
    @DisplayName("bookAppointment with explicit AI_AGENT bookingSource persists AI_AGENT")
    void bookAppointment_explicitAiAgentSource() {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(10L)
                .doctorId(20L)
                .serviceId(30L)
                .startTime(startTime)
                .endTime(endTime)
                .bookingSource(BookingSource.AI_AGENT)
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(service));
        when(appointmentRepository.findOverlappingDoctorAppointments(20L, startTime, endTime)).thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(appointmentResponse);

        appointmentService.bookAppointment(request);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertThat(captor.getValue().getBookingSource()).isEqualTo(BookingSource.AI_AGENT);
    }

    @Test
    @DisplayName("bookAppointment with callRecordId and no explicit bookingSource defaults to AI_AGENT")
    void bookAppointment_callRecordIdDefaultsToAiAgent() {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(10L)
                .doctorId(20L)
                .serviceId(30L)
                .startTime(startTime)
                .endTime(endTime)
                .callRecordId(99L)
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(service));
        when(callRecordRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());
        when(appointmentRepository.findOverlappingDoctorAppointments(20L, startTime, endTime)).thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(appointmentResponse);

        appointmentService.bookAppointment(request);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertThat(captor.getValue().getBookingSource()).isEqualTo(BookingSource.AI_AGENT);
    }

    @Test
    @DisplayName("bookAppointment without callRecordId or explicit bookingSource defaults to MANUAL")
    void bookAppointment_defaultBookingSourceIsManual() {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(10L)
                .doctorId(20L)
                .serviceId(30L)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(service));
        when(appointmentRepository.findOverlappingDoctorAppointments(20L, startTime, endTime)).thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(appointmentResponse);

        appointmentService.bookAppointment(request);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertThat(captor.getValue().getBookingSource()).isEqualTo(BookingSource.MANUAL);
    }

    @Test
    @DisplayName("getAppointments with bookingSource filter queries repository with bookingSource")
    void getAppointments_withBookingSourceFilter() {
        when(appointmentRepository.findFilteredAppointments(eq(1L), any(), any(), any(), eq(BookingSource.AI_AGENT), any(), any()))
                .thenReturn(List.of(appointment));
        when(appointmentMapper.toResponseList(any())).thenReturn(List.of(appointmentResponse));

        List<AppointmentResponse> result = appointmentService.getAppointments(
                1L, null, null, null, null, BookingSource.AI_AGENT);

        assertThat(result).hasSize(1);
        verify(appointmentRepository).findFilteredAppointments(eq(1L), any(), any(), any(), eq(BookingSource.AI_AGENT), any(), any());
    }

    @Test
    @DisplayName("bookAppointment triggers Google Calendar event creation and records eventId")
    void bookAppointment_syncsWithGoogleCalendar() {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(10L)
                .doctorId(20L)
                .serviceId(30L)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(doctor));
        when(serviceRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(service));
        when(appointmentRepository.findOverlappingDoctorAppointments(20L, startTime, endTime)).thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(appointmentResponse);
        when(calendarService.createAppointmentEvent(any(Appointment.class))).thenReturn(Optional.of("gcal_booked_123"));

        appointmentService.bookAppointment(request);

        verify(calendarService).createAppointmentEvent(any(Appointment.class));
        assertThat(appointment.getCalendarEventId()).isEqualTo("gcal_booked_123");
        assertThat(appointment.getCalendarEventAdded()).isTrue();
    }

    @Test
    @DisplayName("updateAppointment triggers Google Calendar update")
    void updateAppointment_reflectsUpdatesOnGoogleCalendar() {
        Appointment existing = Appointment.builder()
                .clinicId(1L)
                .patient(patient)
                .doctor(doctor)
                .service(service)
                .appointmentStart(startTime)
                .appointmentEnd(endTime)
                .calendarEventId("gcal_evt_existing")
                .calendarEventAdded(true)
                .build();
        existing.setId(100L);

        AppointmentRequest request = AppointmentRequest.builder()
                .startTime(startTime.plusDays(1))
                .endTime(endTime.plusDays(1))
                .reason("Rescheduled consultation")
                .build();

        when(appointmentRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(existing));
        when(appointmentRepository.findOverlappingDoctorAppointmentsExcluding(eq(20L), any(), any(), eq(100L)))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(existing);
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(appointmentResponse);

        appointmentService.updateAppointment(100L, request, null);

        verify(calendarService).updateAppointmentEvent(existing);
    }

    @Test
    @DisplayName("updateStatus to COMPLETED updates Google Calendar event")
    void updateStatus_reflectsStatusOnGoogleCalendar() {
        appointment.setCalendarEventId("gcal_status_evt");
        when(appointmentRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(appointmentResponse);

        appointmentService.updateStatus(100L, AppointmentStatus.COMPLETED, null);

        verify(calendarService).updateAppointmentEvent(appointment);
    }

    @Test
    @DisplayName("updateStatus to CANCELLED cancels Google Calendar event")
    void updateStatus_cancelled_cancelsGoogleCalendarEvent() {
        appointment.setCalendarEventId("gcal_cancel_evt");
        when(appointmentRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(appointmentResponse);

        appointmentService.updateStatus(100L, AppointmentStatus.CANCELLED, null);

        verify(calendarService).cancelAppointmentEvent(eq(appointment), anyString());
    }

    @Test
    @DisplayName("cancelAppointment cancels Google Calendar event and notifies attendees")
    void cancelAppointment_cancelsGoogleCalendarEvent() {
        appointment.setCalendarEventId("gcal_cancel_evt");
        when(appointmentRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(appointmentResponse);

        appointmentService.cancelAppointment(100L, "Doctor unavailable", null);

        verify(calendarService).cancelAppointmentEvent(appointment, "Doctor unavailable");
    }

    @Test
    @DisplayName("addCalendarEvent syncs event and saves patient email if provided")
    void addCalendarEvent_syncsEventAndUpdatesPatientEmail() {
        patient.setEmail(null);
        appointment.setPatient(patient);
        appointment.setCalendarEventId(null);
        appointment.setCalendarEventAdded(false);

        when(appointmentRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(appointment));
        when(calendarService.syncAppointmentEvent(appointment)).thenReturn(Optional.of("gcal_synced_999"));

        CalendarEventRequest req = CalendarEventRequest.builder()
                .appointmentId(100L)
                .patientEmail("new.patient@gmail.com")
                .build();

        appointmentService.addCalendarEvent(req);

        assertThat(patient.getEmail()).isEqualTo("new.patient@gmail.com");
        verify(patientRepository).save(patient);
        verify(calendarService).syncAppointmentEvent(appointment);
        assertThat(appointment.getCalendarEventId()).isEqualTo("gcal_synced_999");
        assertThat(appointment.getCalendarEventAdded()).isTrue();
    }

    @Test
    @DisplayName("getAvailableSlots auto-syncs doctor from user repository when not found in doctor repository")
    void getAvailableSlots_autoSyncsFromUserRepository() {
        LocalDate date = LocalDate.of(2026, 10, 25);
        com.dental.manage.entity.User doctorUser = com.dental.manage.entity.User.builder()
                .clinicId(1L)
                .username("doctor")
                .fullName("Dr. Jane Smith")
                .role(com.dental.manage.enums.UserRole.DOCTOR)
                .isActive(true)
                .build();
        doctorUser.setId(20L);

        when(doctorRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.empty());
        when(userRepository.findByIdAndClinicIdAndRoleAndIsDeletedFalse(20L, 1L, com.dental.manage.enums.UserRole.DOCTOR))
                .thenReturn(Optional.of(doctorUser));
        when(doctorManagementService.syncDoctorEntity(doctorUser, "General Dentistry")).thenReturn(doctor);
        when(doctorAvailabilityRepository.findByDoctorIdAndDayOfWeekAndIsActiveTrueAndIsDeletedFalse(eq(20L), any()))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.findByDoctorIdAndDateRange(eq(20L), any(), any()))
                .thenReturn(Collections.emptyList());

        var result = appointmentService.getAvailableSlots(20L, date, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getDoctorId()).isEqualTo(20L);
        verify(doctorManagementService).syncDoctorEntity(doctorUser, "General Dentistry");
    }
}
