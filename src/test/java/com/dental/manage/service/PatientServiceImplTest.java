package com.dental.manage.service;

import com.dental.manage.dto.response.PatientUpcomingAppointmentResponse;
import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.Patient;
import com.dental.manage.entity.Service;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.ConfirmationStatus;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.repository.PatientRepository;
import com.dental.manage.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientServiceImpl patientService;

    private Patient patient;
    private Doctor doctor;
    private Service service;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .clinicId(1L)
                .firstName("Alice")
                .lastName("Johnson")
                .phoneNumber("+1234567890")
                .email("alice@example.com")
                .build();
        patient.setId(10L);

        doctor = Doctor.builder()
                .clinicId(1L)
                .firstName("Bob")
                .lastName("Smith")
                .specialization("Orthodontics")
                .build();
        doctor.setId(20L);

        service = Service.builder()
                .clinicId(1L)
                .name("Teeth Cleaning")
                .durationMinutes(45)
                .price(new BigDecimal("100.00"))
                .build();
        service.setId(30L);

        appointment = Appointment.builder()
                .clinicId(1L)
                .patient(patient)
                .doctor(doctor)
                .service(service)
                .appointmentStart(LocalDateTime.now().plusDays(2))
                .appointmentEnd(LocalDateTime.now().plusDays(2).plusMinutes(45))
                .status(AppointmentStatus.SCHEDULED)
                .confirmationStatus(ConfirmationStatus.CONFIRMED)
                .build();
        appointment.setId(100L);
    }

    @Test
    @DisplayName("getUpcomingAppointments should return lightweight appointment responses without audit fields")
    void getUpcomingAppointments_shouldReturnAppointments() {
        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(patientRepository.findUpcomingAppointmentsByPatientId(eq(10L), any(LocalDateTime.class)))
                .thenReturn(List.of(appointment));

        List<PatientUpcomingAppointmentResponse> result = patientService.getUpcomingAppointments(10L);

        assertThat(result).hasSize(1);
        PatientUpcomingAppointmentResponse resp = result.get(0);
        assertThat(resp.getAppointmentId()).isEqualTo(100L);
        assertThat(resp.getPatientId()).isEqualTo(10L);
        assertThat(resp.getPatientName()).isEqualTo("Alice Johnson");
        assertThat(resp.getPatientPhone()).isEqualTo("+1234567890");
        assertThat(resp.getDoctorId()).isEqualTo(20L);
        assertThat(resp.getDoctorName()).isEqualTo("Bob Smith");
        assertThat(resp.getDoctorSpecialization()).isEqualTo("Orthodontics");
        assertThat(resp.getServiceId()).isEqualTo(30L);
        assertThat(resp.getServiceName()).isEqualTo("Teeth Cleaning");
        assertThat(resp.getServiceDurationMinutes()).isEqualTo(45);
        assertThat(resp.getServicePrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(resp.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(resp.getConfirmationStatus()).isEqualTo(ConfirmationStatus.CONFIRMED);

        verify(patientRepository).findByIdAndIsDeletedFalse(10L);
        verify(patientRepository).findUpcomingAppointmentsByPatientId(eq(10L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("getUpcomingAppointments should throw ResourceNotFoundException when patient does not exist")
    void getUpcomingAppointments_patientNotFound_shouldThrow() {
        when(patientRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getUpcomingAppointments(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found with id: 999");
    }

    @Test
    @DisplayName("getUpcomingAppointments should throw IllegalArgumentException when patientId is null")
    void getUpcomingAppointments_nullId_shouldThrow() {
        assertThatThrownBy(() -> patientService.getUpcomingAppointments(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("patient_id parameter is required");
    }

    @Test
    @DisplayName("getUpcomingAppointments should return empty list when patient has no upcoming appointments")
    void getUpcomingAppointments_noAppointments_shouldReturnEmpty() {
        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(patientRepository.findUpcomingAppointmentsByPatientId(eq(10L), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<PatientUpcomingAppointmentResponse> result = patientService.getUpcomingAppointments(10L);

        assertThat(result).isEmpty();
    }
}
