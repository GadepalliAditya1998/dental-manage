package com.dental.manage.service.impl;

import com.dental.manage.dto.request.PatientLookupRequest;
import com.dental.manage.dto.response.PatientLookupResponse;
import com.dental.manage.entity.User;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.service.PatientLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of PatientLookupService.
 * Restricts queries strictly to PATIENT role, ensuring DOCTOR and RECEPTIONIST roles are not returned.
 * Requires phone number, full name, and date of birth.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PatientLookupServiceImpl implements PatientLookupService {

    private final UserRepository userRepository;

    @Override
    public PatientLookupResponse lookupPatient(PatientLookupRequest request) {
        validateRequest(request);

        log.info("Performing patient lookup with phone: '{}', fullName: '{}', dob: '{}', clinicId: '{}'",
                request.getPhoneNumber(), request.getFullName(), request.getDateOfBirth(), request.getClinicId());

        List<User> patients = userRepository.findPatientsForLookup(
                request.getPhoneNumber().trim(),
                request.getFullName().trim(),
                request.getDateOfBirth(),
                request.getClinicId()
        );

        if (patients.isEmpty()) {
            log.warn("No patient found with phone: '{}', fullName: '{}', dob: '{}'",
                    request.getPhoneNumber(), request.getFullName(), request.getDateOfBirth());
            throw new ResourceNotFoundException(
                    "Patient not found with the provided phone number, full name, and date of birth");
        }

        User patient = patients.get(0);
        log.info("Patient found successfully: id={}, fullName={}, role={}",
                patient.getId(), patient.getFullName(), patient.getRole());

        return mapToResponse(patient);
    }

    private void validateRequest(PatientLookupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Patient lookup request cannot be null");
        }
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is mandatory");
        }
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is mandatory");
        }
        if (request.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Date of birth is mandatory");
        }
        if (request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth must be in the past");
        }
    }

    private PatientLookupResponse mapToResponse(User user) {
        String fullName = user.getFullName() != null ? user.getFullName() : "";
        String firstName = "";
        String lastName = "";

        if (!fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : "";
        }

        return PatientLookupResponse.builder()
                .id(user.getId())
                .clinicId(user.getClinicId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .firstName(firstName)
                .lastName(lastName)
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .isActive(user.getIsActive())
                .createdOn(user.getCreatedOn())
                .modifiedOn(user.getModifiedOn())
                .build();
    }
}
