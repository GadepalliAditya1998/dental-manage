package com.dental.manage.service;

import com.dental.manage.dto.request.DoctorAvailabilityRequest;
import com.dental.manage.dto.request.DoctorRequest;
import com.dental.manage.dto.response.DoctorAvailabilityResponse;
import com.dental.manage.dto.response.DoctorResponse;

import java.util.List;

/**
 * Service for managing doctor profiles and availability schedules.
 */
public interface DoctorManagementService {

    List<DoctorResponse> getDoctors(Long clinicId, Long serviceId, Boolean activeOnly);

    DoctorResponse getDoctorById(Long id, Long clinicId);

    DoctorResponse createDoctor(DoctorRequest request, Long clinicId);

    DoctorResponse updateDoctor(Long id, DoctorRequest request, Long clinicId);

    void deleteDoctor(Long id, Long clinicId);

    List<DoctorAvailabilityResponse> getDoctorAvailability(Long doctorId, Long clinicId);

    List<DoctorAvailabilityResponse> updateDoctorAvailability(Long doctorId, List<DoctorAvailabilityRequest> availabilities, Long clinicId);

    com.dental.manage.entity.Doctor syncDoctorEntity(com.dental.manage.entity.User user, String specialization);
}
