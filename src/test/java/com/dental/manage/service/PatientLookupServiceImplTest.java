package com.dental.manage.service;

import com.dental.manage.dto.request.PatientLookupRequest;
import com.dental.manage.dto.response.PatientLookupResponse;
import com.dental.manage.entity.User;
import com.dental.manage.enums.UserRole;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.service.impl.PatientLookupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientLookupServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PatientLookupServiceImpl patientLookupService;

    private User samplePatient;

    @BeforeEach
    void setUp() {
        samplePatient = User.builder()
                .clinicId(1L)
                .patientId("PAT-1-0192B6E9F8A3B4C1")
                .username("patient_john")
                .password("passwordHash")
                .role(UserRole.PATIENT)
                .fullName("John Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1-555-0199")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .isActive(true)
                .build();
        samplePatient.setId(100L);
        samplePatient.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        samplePatient.setModifiedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    @Test
    @DisplayName("Lookup patient successfully with unique string patientId")
    void lookupPatient_byPatientId_success_shouldReturnPatientDetails() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .patientId("PAT-1-0192B6E9F8A3B4C1")
                .clinicId(1L)
                .build();

        when(userRepository.findPatientsByPatientId("PAT-1-0192B6E9F8A3B4C1", 1L))
                .thenReturn(List.of(samplePatient));

        PatientLookupResponse response = patientLookupService.lookupPatient(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getPatientId()).isEqualTo("PAT-1-0192B6E9F8A3B4C1");
        assertThat(response.getClinicId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("John Doe");
        assertThat(response.getPhoneNumber()).isEqualTo("+1-555-0199");
        assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(response.getRole()).isEqualTo("PATIENT");

        verify(userRepository).findPatientsByPatientId("PAT-1-0192B6E9F8A3B4C1", 1L);
    }

    @Test
    @DisplayName("Lookup patient by patientId extracts clinicId automatically when clinicId is null in request")
    void lookupPatient_byPatientId_shouldExtractClinicIdFromIdPrefix() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .patientId("PAT-2-000ABCDEF1234567")
                .build();

        samplePatient.setClinicId(2L);
        samplePatient.setPatientId("PAT-2-000ABCDEF1234567");

        when(userRepository.findPatientsByPatientId("PAT-2-000ABCDEF1234567", 2L))
                .thenReturn(List.of(samplePatient));

        PatientLookupResponse response = patientLookupService.lookupPatient(request);

        assertThat(response).isNotNull();
        assertThat(response.getPatientId()).isEqualTo("PAT-2-000ABCDEF1234567");
        assertThat(response.getClinicId()).isEqualTo(2L);

        verify(userRepository).findPatientsByPatientId("PAT-2-000ABCDEF1234567", 2L);
    }

    @Test
    @DisplayName("Lookup patient successfully with mobile number and date of birth (no fullName needed)")
    void lookupPatient_byPhoneAndDob_success_shouldReturnPatientDetails() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("+1-555-0199")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .clinicId(1L)
                .build();

        when(userRepository.findPatientsByPhoneAndDob("+1-555-0199", LocalDate.of(1990, 5, 15), 1L))
                .thenReturn(List.of(samplePatient));

        PatientLookupResponse response = patientLookupService.lookupPatient(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getPatientId()).isEqualTo("PAT-1-0192B6E9F8A3B4C1");
        assertThat(response.getClinicId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("John Doe");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getPhoneNumber()).isEqualTo("+1-555-0199");
        assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(response.getRole()).isEqualTo("PATIENT");
        assertThat(response.getIsActive()).isTrue();

        verify(userRepository).findPatientsByPhoneAndDob("+1-555-0199", LocalDate.of(1990, 5, 15), 1L);
    }

    @Test
    @DisplayName("Lookup patient by patientId not found throws ResourceNotFoundException")
    void lookupPatient_byPatientId_notFound_shouldThrowResourceNotFoundException() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .patientId("PAT-1-NONEXISTENT")
                .clinicId(1L)
                .build();

        when(userRepository.findPatientsByPatientId("PAT-1-NONEXISTENT", 1L))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> patientLookupService.lookupPatient(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found with the provided patient ID: PAT-1-NONEXISTENT");
    }

    @Test
    @DisplayName("Lookup patient by phone and DOB not found throws ResourceNotFoundException")
    void lookupPatient_byPhoneAndDob_notFound_shouldThrowResourceNotFoundException() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("+1-555-9999")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .clinicId(1L)
                .build();

        when(userRepository.findPatientsByPhoneAndDob(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> patientLookupService.lookupPatient(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found with the provided phone number and date of birth");
    }

    @Test
    @DisplayName("Lookup patient missing phone number when patientId is null throws IllegalArgumentException")
    void lookupPatient_missingPhoneNumber_shouldThrowIllegalArgumentException() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber(null)
                .fullName("John Doe")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .build();

        assertThatThrownBy(() -> patientLookupService.lookupPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number is mandatory");

        verify(userRepository, never()).findPatientsByPhoneAndDob(any(), any(), any());
    }

    @Test
    @DisplayName("Lookup patient blank phone number when patientId is null throws IllegalArgumentException")
    void lookupPatient_blankPhoneNumber_shouldThrowIllegalArgumentException() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("   ")
                .fullName("John Doe")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .build();

        assertThatThrownBy(() -> patientLookupService.lookupPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number is mandatory");

        verify(userRepository, never()).findPatientsByPhoneAndDob(any(), any(), any());
    }

    @Test
    @DisplayName("Lookup patient missing date of birth when patientId is null throws IllegalArgumentException")
    void lookupPatient_missingDateOfBirth_shouldThrowIllegalArgumentException() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("+1-555-0199")
                .fullName("John Doe")
                .dateOfBirth(null)
                .build();

        assertThatThrownBy(() -> patientLookupService.lookupPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date of birth is mandatory");

        verify(userRepository, never()).findPatientsByPhoneAndDob(any(), any(), any());
    }

    @Test
    @DisplayName("Lookup patient missing both patientId and (phone, DOB) throws IllegalArgumentException")
    void lookupPatient_missingBothPatientIdAndPhoneDob_shouldThrowIllegalArgumentException() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .fullName("John Doe")
                .build();

        assertThatThrownBy(() -> patientLookupService.lookupPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Either patientId or both mobile number and date of birth must be provided");

        verify(userRepository, never()).findPatientsByPhoneAndDob(any(), any(), any());
        verify(userRepository, never()).findPatientsByPatientId(any(), any());
    }

    @Test
    @DisplayName("Lookup patient with future date of birth throws IllegalArgumentException")
    void lookupPatient_futureDateOfBirth_shouldThrowIllegalArgumentException() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("+1-555-0199")
                .fullName("John Doe")
                .dateOfBirth(LocalDate.now().plusDays(1))
                .build();

        assertThatThrownBy(() -> patientLookupService.lookupPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date of birth must be in the past");

        verify(userRepository, never()).findPatientsByPhoneAndDob(any(), any(), any());
    }

    @Test
    @DisplayName("Lookup patient with null request throws IllegalArgumentException")
    void lookupPatient_nullRequest_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> patientLookupService.lookupPatient(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Patient lookup request cannot be null");
    }
}
