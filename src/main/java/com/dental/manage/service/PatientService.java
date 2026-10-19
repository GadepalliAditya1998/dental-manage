package com.dental.manage.service;

import com.dental.manage.dto.response.PatientUpcomingAppointmentResponse;

import java.util.List;

/**
 * Service interface for patient management and appointment inquiries.
 */
public interface PatientService {

    /**
     * Fetches all upcoming appointments for a given patient ID.
     * Returns lightweight records without audit fields, ordered chronologically.
     *
     * @param patientId Patient identifier
     * @return List of PatientUpcomingAppointmentResponse
     */
    List<PatientUpcomingAppointmentResponse> getUpcomingAppointments(Long patientId);
}
