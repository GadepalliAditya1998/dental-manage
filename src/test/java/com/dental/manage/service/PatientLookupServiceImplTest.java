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
import static org.mockito.ArgumentMatchers.eq;
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
    private PatientLookupRequest validRequest;

    @BeforeEach
    void setUp() {
        samplePatient = User.builder()
                .clinicId(1L)
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

        validRequest = PatientLookupRequest.builder()
                .phoneNumber("+1-555-0199")
                .fullName("John Doe")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .clinicId(1L)
                .build();
    }

    @Test
    @DisplayName("Lookup patient successfully with all mandatory parameters")
    void lookupPatient_success_shouldReturnPatientDetails() {
        when(userRepository.findPatientsForLookup("+1-555-0199", "John Doe", LocalDate.of(1990, 5, 15), 1L))
                .thenReturn(List.of(samplePatient));

        PatientLookupResponse response = patientLookupService.lookupPatient(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getClinicId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("John Doe");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getPhoneNumber()).isEqualTo("+1-555-0199");
        assertThat(response.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(response.getRole()).isEqualTo("PATIENT");
        assertThat(response.getIsActive()).isTrue();

        verify(userRepository).findPatientsForLookup("+1-555-0199", "John Doe", LocalDate.of(1990, 5, 15), 1L);
    }

    @Test
    @DisplayName("Lookup patient not found throws ResourceNotFoundException")
    void lookupPatient_notFound_shouldThrowResourceNotFoundException() {
        when(userRepository.findPatientsForLookup(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> patientLookupService.lookupPatient(validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found with the provided phone number, full name, and date of birth");
    }

    @Test
    @DisplayName("Lookup patient missing phone number throws IllegalArgumentException")
    void lookupPatient_missingPhoneNumber_shouldThrowIllegalArgumentException() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber(null)
                .fullName("John Doe")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .build();

        assertThatThrownBy(() -> patientLookupService.lookupPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number is mandatory");

        verify(userRepository, never()).findPatientsForLookup(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Lookup patient blank phone number throws IllegalArgumentException")
    void lookupPatient_blankPhoneNumber_shouldThrowIllegalArgumentException() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("   ")
                .fullName("John Doe")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .build();

        assertThatThrownBy(() -> patientLookupService.lookupPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number is mandatory");

        verify(userRepository, never()).findPatientsForLookup(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Lookup patient missing full name throws IllegalArgumentException")
    void lookupPatient_missingFullName_shouldThrowIllegalArgumentException() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("+1-555-0199")
                .fullName(null)
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .build();

        assertThatThrownBy(() -> patientLookupService.lookupPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Full name is mandatory");

        verify(userRepository, never()).findPatientsForLookup(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Lookup patient missing date of birth throws IllegalArgumentException")
    void lookupPatient_missingDateOfBirth_shouldThrowIllegalArgumentException() {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("+1-555-0199")
                .fullName("John Doe")
                .dateOfBirth(null)
                .build();

        assertThatThrownBy(() -> patientLookupService.lookupPatient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date of birth is mandatory");

        verify(userRepository, never()).findPatientsForLookup(any(), any(), any(), any());
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

        verify(userRepository, never()).findPatientsForLookup(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Lookup patient with null request throws IllegalArgumentException")
    void lookupPatient_nullRequest_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> patientLookupService.lookupPatient(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Patient lookup request cannot be null");
    }
}
