package com.dental.manage.service.impl;

import com.dental.manage.dto.response.PatientUpcomingAppointmentResponse;
import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.Patient;
import com.dental.manage.entity.Service;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.repository.PatientRepository;
import com.dental.manage.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of PatientService.
 * Handles patient queries and retrieves upcoming appointments without audit fields.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    @Override
    public List<PatientUpcomingAppointmentResponse> getUpcomingAppointments(Long patientId) {
        if (patientId == null) {
            throw new IllegalArgumentException("patient_id parameter is required");
        }

        log.info("Fetching upcoming appointments for patient_id: {}", patientId);

        // Verify patient exists and is not soft-deleted
        Patient patient = patientRepository.findByIdAndIsDeletedFalse(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        LocalDateTime now = LocalDateTime.now();
        List<Appointment> appointments = patientRepository.findUpcomingAppointmentsByPatientId(patientId, now);

        log.info("Found {} upcoming appointments for patient_id: {}", appointments.size(), patientId);

        return appointments.stream()
                .map(this::mapToUpcomingResponse)
                .toList();
    }

    private PatientUpcomingAppointmentResponse mapToUpcomingResponse(Appointment a) {
        Patient p = a.getPatient();
        Doctor d = a.getDoctor();
        Service s = a.getService();

        String patientFullName = (p != null) ? formatFullName(p.getFirstName(), p.getLastName()) : null;
        String doctorFullName = (d != null) ? formatFullName(d.getFirstName(), d.getLastName()) : null;

        return PatientUpcomingAppointmentResponse.builder()
                .appointmentId(a.getId())
                .patientId(p != null ? p.getId() : null)
                .patientName(patientFullName)
                .patientPhone(p != null ? p.getPhoneNumber() : null)
                .doctorId(d != null ? d.getId() : null)
                .doctorName(doctorFullName)
                .doctorSpecialization(d != null ? d.getSpecialization() : null)
                .serviceId(s != null ? s.getId() : null)
                .serviceName(s != null ? s.getName() : null)
                .serviceDurationMinutes(s != null ? s.getDurationMinutes() : null)
                .servicePrice(s != null ? s.getPrice() : null)
                .appointmentStart(a.getAppointmentStart())
                .appointmentEnd(a.getAppointmentEnd())
                .status(a.getStatus())
                .confirmationStatus(a.getConfirmationStatus())
                .build();
    }

    private String formatFullName(String firstName, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return firstName != null ? firstName.trim() : "";
        }
        return (firstName != null ? firstName.trim() : "") + " " + lastName.trim();
    }
}
