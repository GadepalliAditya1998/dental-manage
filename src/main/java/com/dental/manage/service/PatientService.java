package com.dental.manage.service;

import com.dental.manage.dto.request.PatientRequest;
import com.dental.manage.dto.response.PatientResponse;
import com.dental.manage.dto.response.PatientUpcomingAppointmentResponse;

import java.util.List;

/**
 * Service interface for patient management and appointment inquiries.
 */
public interface PatientService {

    /**
     * Creates a new patient record and associated user account with a secure random password.
     *
     * @param request PatientRequest containing patient name, email, phone number, date of birth
     * @return PatientResponse with the created patient details, username (email), and generated password
     */
    PatientResponse createPatient(PatientRequest request);

    /**
     * Fetches all non-deleted patients, optionally filtered by clinicId and search query.
     *
     * @param clinicId optional clinicId filter
     * @param search optional search keyword matching name, email, phone, or patientId
     * @return List of PatientResponse
     */
    List<PatientResponse> getPatients(Long clinicId, String search);

    /**
     * Retrieves a patient by ID, optionally scoped to clinicId.
     *
     * @param id Patient ID
     * @param clinicId optional clinicId filter
     * @return PatientResponse
     */
    PatientResponse getPatientById(Long id, Long clinicId);

    /**
     * Updates an existing patient record and synchronizes associated user account details.
     *
     * @param id Patient ID
     * @param request PatientRequest containing updated details
     * @param clinicId optional clinicId filter
     * @return Updated PatientResponse
     */
    PatientResponse updatePatient(Long id, PatientRequest request, Long clinicId);

    /**
     * Soft deletes a patient and associated user account.
     *
     * @param id Patient ID
     * @param clinicId optional clinicId filter
     */
    void deletePatient(Long id, Long clinicId);

    /**
     * Fetches all upcoming appointments for a given patient ID.
     * Returns lightweight records without audit fields, ordered chronologically.
     *
     * @param patientId Patient identifier
     * @return List of PatientUpcomingAppointmentResponse
     */
    List<PatientUpcomingAppointmentResponse> getUpcomingAppointments(Long patientId);
}
