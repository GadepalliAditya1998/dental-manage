package com.dental.manage.service;

import com.dental.manage.dto.request.PatientLookupRequest;
import com.dental.manage.dto.response.PatientLookupResponse;

/**
 * Service interface for patient lookup operations.
 */
public interface PatientLookupService {

    /**
     * Looks up an existing patient using:
     * 1) A unique string identifier (patientId)
     * OR
     * 2) Patient's mobile number and date of birth.
     *
     * Restricted to PATIENT role only (excludes DOCTOR and RECEPTIONIST).
     *
     * @param request PatientLookupRequest containing patientId or (phoneNumber + dateOfBirth)
     * @return PatientLookupResponse containing patient details
     * @throws IllegalArgumentException if required fields are missing or invalid
     * @throws com.dental.manage.exception.ResourceNotFoundException if no matching patient is found
     */
    PatientLookupResponse lookupPatient(PatientLookupRequest request);
}
