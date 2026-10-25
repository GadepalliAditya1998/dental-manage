package com.dental.manage.service.impl;

import com.dental.manage.dto.request.PatientLookupRequest;
import com.dental.manage.dto.response.PatientLookupResponse;
import com.dental.manage.entity.User;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.service.PatientLookupService;
import com.dental.manage.util.PatientIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of PatientLookupService.
 * Restricts queries strictly to PATIENT role, ensuring DOCTOR and RECEPTIONIST roles are not returned.
 * Supports lookup by unique string patientId OR (mobile number + date of birth).
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

        Long effectiveClinicId = request.getClinicId();
        List<User> patients;

        if (request.getPatientId() != null && !request.getPatientId().trim().isEmpty()) {
            String patientId = request.getPatientId().trim();
            if (effectiveClinicId == null) {
                effectiveClinicId = PatientIdGenerator.extractClinicId(patientId);
            }
            log.info("Performing patient lookup with patientId: '{}', clinicId: '{}'", patientId, effectiveClinicId);
            patients = userRepository.findPatientsByPatientId(patientId, effectiveClinicId);

            if (patients.isEmpty()) {
                log.warn("No patient found with patientId: '{}', clinicId: '{}'", patientId, effectiveClinicId);
                throw new ResourceNotFoundException("Patient not found with the provided patient ID: " + patientId);
            }
        } else {
            String phone = request.getPhoneNumber().trim();
            LocalDate dob = request.getDateOfBirth();
            log.info("Performing patient lookup with phone: '{}', dob: '{}', clinicId: '{}'",
                    phone, dob, effectiveClinicId);

            patients = userRepository.findPatientsByPhoneAndDob(phone, dob, effectiveClinicId);

            if (patients.isEmpty()) {
                log.warn("No patient found with phone: '{}', dob: '{}'", phone, dob);
                throw new ResourceNotFoundException(
                        "Patient not found with the provided phone number and date of birth");
            }
        }

        User patient = patients.get(0);
        log.info("Patient found successfully: id={}, patientId={}, fullName={}, role={}",
                patient.getId(), patient.getPatientId(), patient.getFullName(), patient.getRole());

        return mapToResponse(patient);
    }

    private void validateRequest(PatientLookupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Patient lookup request cannot be null");
        }

        boolean hasPatientId = request.getPatientId() != null && !request.getPatientId().trim().isEmpty();
        boolean hasPhone = request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty();
        boolean hasDob = request.getDateOfBirth() != null;

        if (hasPatientId) {
            return;
        }

        if (!hasPhone && !hasDob) {
            throw new IllegalArgumentException("Either patientId or both mobile number and date of birth must be provided");
        }
        if (!hasPhone) {
            throw new IllegalArgumentException("Phone number is mandatory");
        }
        if (!hasDob) {
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

        String effectivePatientId = user.getPatientId() != null && !user.getPatientId().isBlank()
                ? user.getPatientId()
                : (user.getClinicId() != null ? "PAT-" + user.getClinicId() + "-" + user.getId() : "PAT-1-" + user.getId());

        return PatientLookupResponse.builder()
                .id(user.getId())
                .patientId(effectivePatientId)
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
