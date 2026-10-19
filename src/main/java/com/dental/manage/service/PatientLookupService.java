package com.dental.manage.service;

import com.dental.manage.dto.request.PatientLookupRequest;
import com.dental.manage.dto.response.PatientLookupResponse;

/**
 * Service interface for patient lookup operations.
 */
public interface PatientLookupService {

    /**
     * Looks up an existing patient using phone number, full name, and date of birth.
     * All three parameters are mandatory.
     * Restricted to PATIENT role only (excludes DOCTOR and RECEPTIONIST).
     *
     * @param request PatientLookupRequest containing mandatory search parameters
     * @return PatientLookupResponse containing patient details
     * @throws IllegalArgumentException if mandatory fields are missing or invalid
     * @throws com.dental.manage.exception.ResourceNotFoundException if no matching patient is found
     */
    PatientLookupResponse lookupPatient(PatientLookupRequest request);
}
