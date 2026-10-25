package com.dental.manage.service;

import com.dental.manage.dto.request.PatientRequest;
import com.dental.manage.dto.response.PatientResponse;
import com.dental.manage.dto.response.PatientUpcomingAppointmentResponse;
import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.Patient;
import com.dental.manage.entity.Service;
import com.dental.manage.entity.User;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.ConfirmationStatus;
import com.dental.manage.enums.UserRole;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.repository.PatientRepository;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.service.impl.PatientServiceImpl;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

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

    // =========================================================================
    // createPatient Tests
    // =========================================================================

    @Test
    @DisplayName("createPatient should save patient and user, set username=email, and generate 8-char alphanumeric password")
    void createPatient_validRequest_shouldCreatePatientAndUser() {
        PatientRequest request = PatientRequest.builder()
                .name("Alice Johnson")
                .email("alice.j@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1995, 6, 20))
                .clinicId(2L)
                .build();

        Patient savedPatient = Patient.builder()
                .clinicId(2L)
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice.j@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1995, 6, 20))
                .isNewLead(true)
                .build();
        savedPatient.setId(50L);

        when(userRepository.existsByUsernameAndIsDeletedFalse("alice.j@example.com")).thenReturn(false);
        when(patientRepository.findByPhoneNumberAndIsDeletedFalse("+1234567890")).thenReturn(Optional.empty());
        when(userRepository.existsByPassword(anyString())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);

        PatientResponse response = patientService.createPatient(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getClinicId()).isEqualTo(2L);
        assertThat(response.getFirstName()).isEqualTo("Alice");
        assertThat(response.getLastName()).isEqualTo("Johnson");
        assertThat(response.getFullName()).isEqualTo("Alice Johnson");
        assertThat(response.getEmail()).isEqualTo("alice.j@example.com");
        assertThat(response.getPhoneNumber()).isEqualTo("+1234567890");
        assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 6, 20));
        assertThat(response.getUsername()).isEqualTo("alice.j@example.com");

        // Verify password is exactly 8 alphanumeric characters
        assertThat(response.getPassword()).isNotNull().hasSize(8);
        assertThat(response.getPassword()).matches("^[A-Za-z0-9]{8}$");

        // Verify User saved
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("alice.j@example.com");
        assertThat(savedUser.getPassword()).isEqualTo(response.getPassword());
        assertThat(savedUser.getRole()).isEqualTo(UserRole.PATIENT);
        assertThat(savedUser.getFullName()).isEqualTo("Alice Johnson");
        assertThat(savedUser.getEmail()).isEqualTo("alice.j@example.com");
        assertThat(savedUser.getPhoneNumber()).isEqualTo("+1234567890");
        assertThat(savedUser.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 6, 20));
        assertThat(savedUser.getClinicId()).isEqualTo(2L);
        assertThat(savedUser.getIsActive()).isTrue();

        // Verify Patient saved
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        Patient capturedPatient = patientCaptor.getValue();
        assertThat(capturedPatient.getFirstName()).isEqualTo("Alice");
        assertThat(capturedPatient.getLastName()).isEqualTo("Johnson");
        assertThat(capturedPatient.getEmail()).isEqualTo("alice.j@example.com");
        assertThat(capturedPatient.getPhoneNumber()).isEqualTo("+1234567890");
        assertThat(capturedPatient.getDateOfBirth()).isEqualTo(LocalDate.of(1995, 6, 20));
        assertThat(capturedPatient.getClinicId()).isEqualTo(2L);
        assertThat(capturedPatient.getIsNewLead()).isTrue();
        assertThat(capturedPatient.getPatientId()).isNotNull().startsWith("PAT-2-");
        assertThat(savedUser.getPatientId()).isEqualTo(capturedPatient.getPatientId());
        assertThat(response.getPatientId()).isEqualTo(capturedPatient.getPatientId());
    }

    @Test
    @DisplayName("createPatient with default clinicId should use 1L when clinicId is not provided")
    void createPatient_defaultClinicId_shouldUse1L() {
        PatientRequest request = PatientRequest.builder()
                .firstName("Bob")
                .lastName("Smith")
                .email("bob@example.com")
                .phoneNumber("+1987654321")
                .dateOfBirth(LocalDate.of(1988, 3, 10))
                .build();

        Patient savedPatient = Patient.builder()
                .clinicId(1L)
                .firstName("Bob")
                .lastName("Smith")
                .email("bob@example.com")
                .phoneNumber("+1987654321")
                .dateOfBirth(LocalDate.of(1988, 3, 10))
                .isNewLead(true)
                .build();
        savedPatient.setId(51L);

        when(userRepository.existsByUsernameAndIsDeletedFalse("bob@example.com")).thenReturn(false);
        when(patientRepository.findByPhoneNumberAndIsDeletedFalse("+1987654321")).thenReturn(Optional.empty());
        when(userRepository.existsByPassword(anyString())).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);

        PatientResponse response = patientService.createPatient(request);

        assertThat(response.getClinicId()).isEqualTo(1L);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getClinicId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("createPatient should throw IllegalArgumentException when user with username/email already exists")
    void createPatient_duplicateEmail_shouldThrow() {
        PatientRequest request = PatientRequest.builder()
                .name("Alice Johnson")
                .email("alice@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1995, 6, 20))
                .build();

        when(userRepository.existsByUsernameAndIsDeletedFalse("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> patientService.createPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A user with username/email 'alice@example.com' already exists");

        verify(patientRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPatient should throw IllegalArgumentException when patient with phone number already exists")
    void createPatient_duplicatePhone_shouldThrow() {
        PatientRequest request = PatientRequest.builder()
                .name("Alice Johnson")
                .email("alice@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1995, 6, 20))
                .build();

        when(userRepository.existsByUsernameAndIsDeletedFalse("alice@example.com")).thenReturn(false);
        when(patientRepository.findByPhoneNumberAndIsDeletedFalse("+1234567890")).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> patientService.createPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A patient with phone number '+1234567890' already exists");

        verify(patientRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPatient should throw IllegalArgumentException when request is null")
    void createPatient_nullRequest_shouldThrow() {
        assertThatThrownBy(() -> patientService.createPatient(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Patient request cannot be null");
    }

    @Test
    @DisplayName("createPatient should throw IllegalArgumentException when name is missing")
    void createPatient_missingName_shouldThrow() {
        PatientRequest request = PatientRequest.builder()
                .email("alice@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1995, 6, 20))
                .build();

        assertThatThrownBy(() -> patientService.createPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Patient name is required");
    }

    @Test
    @DisplayName("createPatient should throw IllegalArgumentException when email is missing")
    void createPatient_missingEmail_shouldThrow() {
        PatientRequest request = PatientRequest.builder()
                .name("Alice Johnson")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.of(1995, 6, 20))
                .build();

        assertThatThrownBy(() -> patientService.createPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is required");
    }

    @Test
    @DisplayName("createPatient should throw IllegalArgumentException when phone number is missing")
    void createPatient_missingPhone_shouldThrow() {
        PatientRequest request = PatientRequest.builder()
                .name("Alice Johnson")
                .email("alice@example.com")
                .dateOfBirth(LocalDate.of(1995, 6, 20))
                .build();

        assertThatThrownBy(() -> patientService.createPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number is required");
    }

    @Test
    @DisplayName("createPatient should throw IllegalArgumentException when date of birth is missing")
    void createPatient_missingDob_shouldThrow() {
        PatientRequest request = PatientRequest.builder()
                .name("Alice Johnson")
                .email("alice@example.com")
                .phoneNumber("+1234567890")
                .build();

        assertThatThrownBy(() -> patientService.createPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date of birth is required");
    }

    @Test
    @DisplayName("createPatient should throw IllegalArgumentException when date of birth is in the future")
    void createPatient_futureDob_shouldThrow() {
        PatientRequest request = PatientRequest.builder()
                .name("Alice Johnson")
                .email("alice@example.com")
                .phoneNumber("+1234567890")
                .dateOfBirth(LocalDate.now().plusDays(1))
                .build();

        assertThatThrownBy(() -> patientService.createPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date of birth must be in the past");
    }

    // =========================================================================
    // Existing getUpcomingAppointments Tests
    // =========================================================================

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

    // =========================================================================
    // getPatients Tests
    // =========================================================================

    @Test
    @DisplayName("getPatients should return mapped patient responses")
    void getPatients_shouldReturnList() {
        when(patientRepository.findFilteredPatients(null, null)).thenReturn(List.of(patient));

        List<PatientResponse> responses = patientService.getPatients(null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getFirstName()).isEqualTo("Alice");
        assertThat(responses.get(0).getFullName()).isEqualTo("Alice Johnson");
    }

    @Test
    @DisplayName("getPatients with search and clinicId should query repository accordingly")
    void getPatients_withClinicAndSearch_shouldQuery() {
        when(patientRepository.findFilteredPatients(1L, "Alice")).thenReturn(List.of(patient));

        List<PatientResponse> responses = patientService.getPatients(1L, "Alice");

        assertThat(responses).hasSize(1);
        verify(patientRepository).findFilteredPatients(1L, "Alice");
    }

    // =========================================================================
    // getPatientById Tests
    // =========================================================================

    @Test
    @DisplayName("getPatientById should return patient response when found")
    void getPatientById_found_shouldReturn() {
        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));

        PatientResponse resp = patientService.getPatientById(10L, null);

        assertThat(resp.getId()).isEqualTo(10L);
        assertThat(resp.getFirstName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("getPatientById should throw ResourceNotFoundException when not found")
    void getPatientById_notFound_shouldThrow() {
        when(patientRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatientById(999L, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found with id: 999");
    }

    // =========================================================================
    // updatePatient Tests
    // =========================================================================

    @Test
    @DisplayName("updatePatient should update details and return updated patient")
    void updatePatient_valid_shouldUpdate() {
        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientRequest request = PatientRequest.builder()
                .firstName("Alicia")
                .lastName("Smith")
                .phoneNumber("+1987654321")
                .email("alicia@example.com")
                .dateOfBirth(LocalDate.of(1992, 4, 10))
                .build();

        PatientResponse resp = patientService.updatePatient(10L, request, null);

        assertThat(resp.getFirstName()).isEqualTo("Alicia");
        assertThat(resp.getLastName()).isEqualTo("Smith");
        assertThat(resp.getPhoneNumber()).isEqualTo("+1987654321");
        assertThat(resp.getEmail()).isEqualTo("alicia@example.com");
    }

    // =========================================================================
    // deletePatient Tests
    // =========================================================================

    @Test
    @DisplayName("deletePatient should mark patient as deleted")
    void deletePatient_shouldSoftDelete() {
        when(patientRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(patient));

        patientService.deletePatient(10L, null);

        assertThat(patient.getIsDeleted()).isTrue();
        verify(patientRepository).save(patient);
    }
}
