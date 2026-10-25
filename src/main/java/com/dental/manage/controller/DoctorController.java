package com.dental.manage.controller;

import com.dental.manage.context.ClinicRequestContext;
import com.dental.manage.dto.request.DoctorAvailabilityRequest;
import com.dental.manage.dto.request.DoctorRequest;
import com.dental.manage.dto.response.ApiResponse;
import com.dental.manage.dto.response.DoctorAvailabilityResponse;
import com.dental.manage.dto.response.DoctorResponse;
import com.dental.manage.service.DoctorManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for managing doctors and their working schedule availabilities.
 * Supports multi-tenant scoping via path variable (/api/clinics/{clinicId}/doctors)
 * or query parameter (?clinicId=1).
 */
@RestController
@RequestMapping({"/api/doctors", "/api/doctor", "/api/clinics/{clinicId}/doctors"})
@RequiredArgsConstructor
@Slf4j
public class DoctorController {

    private final DoctorManagementService doctorManagementService;

    /**
     * Retrieve list of doctors, optionally filtered by service and active status.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getDoctors(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId,
            @RequestParam(name = "serviceId", required = false) Long serviceId,
            @RequestParam(name = "activeOnly", required = false, defaultValue = "false") boolean activeOnly) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        log.info("Request received to fetch doctors: clinicId={}, serviceId={}, activeOnly={}", clinicId, serviceId, activeOnly);

        List<DoctorResponse> doctors = doctorManagementService.getDoctors(clinicId, serviceId, activeOnly);
        return ResponseEntity.ok(ApiResponse.success("Doctors fetched successfully", doctors));
    }

    /**
     * Retrieve doctor details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(
            @PathVariable(name = "id") Long id,
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        log.info("Request received to fetch doctor with id: {} (clinicId={})", id, clinicId);

        DoctorResponse doctor = doctorManagementService.getDoctorById(id, clinicId);
        return ResponseEntity.ok(ApiResponse.success("Doctor fetched successfully", doctor));
    }

    /**
     * Create a new doctor profile.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DoctorResponse>> createDoctor(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId,
            @Valid @RequestBody DoctorRequest request) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        log.info("Request received to create doctor: {} {}", request.getFirstName(), request.getLastName());

        DoctorResponse response = doctorManagementService.createDoctor(request, clinicId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Doctor created successfully", response));
    }

    /**
     * Update an existing doctor profile.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateDoctor(
            @PathVariable(name = "id") Long id,
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId,
            @Valid @RequestBody DoctorRequest request) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        log.info("Request received to update doctor with id: {}", id);

        DoctorResponse response = doctorManagementService.updateDoctor(id, request, clinicId);
        return ResponseEntity.ok(ApiResponse.success("Doctor updated successfully", response));
    }

    /**
     * Soft-delete a doctor profile.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDoctor(
            @PathVariable(name = "id") Long id,
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        log.info("Request received to delete doctor with id: {}", id);

        doctorManagementService.deleteDoctor(id, clinicId);
        return ResponseEntity.ok(ApiResponse.success("Doctor deleted successfully", null));
    }

    /**
     * Get weekly availability schedule for a doctor.
     */
    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<List<DoctorAvailabilityResponse>>> getDoctorAvailability(
            @PathVariable(name = "id") Long id,
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        log.info("Request received to get availability for doctor id: {}", id);

        List<DoctorAvailabilityResponse> availabilities = doctorManagementService.getDoctorAvailability(id, clinicId);
        return ResponseEntity.ok(ApiResponse.success("Doctor availability retrieved successfully", availabilities));
    }

    /**
     * Update weekly availability schedule for a doctor.
     */
    @PutMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<List<DoctorAvailabilityResponse>>> updateDoctorAvailability(
            @PathVariable(name = "id") Long id,
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId,
            @RequestBody List<DoctorAvailabilityRequest> availabilities) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        log.info("Request received to update availability for doctor id: {}", id);

        List<DoctorAvailabilityResponse> response = doctorManagementService.updateDoctorAvailability(id, availabilities, clinicId);
        return ResponseEntity.ok(ApiResponse.success("Doctor availability updated successfully", response));
    }
}
