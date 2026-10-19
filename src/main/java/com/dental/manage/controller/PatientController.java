package com.dental.manage.controller;

import com.dental.manage.dto.request.PatientLookupRequest;
import com.dental.manage.dto.response.ApiResponse;
import com.dental.manage.dto.response.PatientLookupResponse;
import com.dental.manage.service.PatientLookupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controller for patient operations including patient lookup.
 * Supports patient lookup by phone number, full name, and date of birth (all mandatory).
 * Restricted to PATIENT role only (excludes DOCTOR and RECEPTIONIST).
 */
@RestController
@RequestMapping({"/api/patients", "/api/patient", "/api/clinics/{clinicId}/patients"})
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientLookupService patientLookupService;

    /**
     * Patient lookup via HTTP GET with query parameters.
     * Mandatory parameters: phoneNumber, fullName, dateOfBirth.
     *
     * @param pathClinicId optional clinicId from path variable (/api/clinics/{clinicId}/patients/lookup)
     * @param phoneNumber patient phone number
     * @param phone alias for phoneNumber
     * @param fullName patient full name
     * @param name alias for fullName
     * @param dateOfBirth patient date of birth (YYYY-MM-DD)
     * @param dob alias for dateOfBirth
     * @param queryClinicId optional clinicId from query parameter
     * @return ApiResponse containing PatientLookupResponse
     */
    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<PatientLookupResponse>> lookupPatient(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "fullName", required = false) String fullName,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "dateOfBirth", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam(name = "dob", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId) {

        String effectivePhone = (phoneNumber != null && !phoneNumber.isBlank()) ? phoneNumber : phone;
        String effectiveFullName = (fullName != null && !fullName.isBlank()) ? fullName : name;
        LocalDate effectiveDob = dateOfBirth != null ? dateOfBirth : dob;
        Long effectiveClinicId = pathClinicId != null ? pathClinicId : queryClinicId;

        if (effectivePhone == null || effectivePhone.isBlank()) {
            throw new IllegalArgumentException("Phone number is mandatory");
        }
        if (effectiveFullName == null || effectiveFullName.isBlank()) {
            throw new IllegalArgumentException("Full name is mandatory");
        }
        if (effectiveDob == null) {
            throw new IllegalArgumentException("Date of birth is mandatory");
        }
        if (effectiveDob.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth must be in the past");
        }

        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber(effectivePhone.trim())
                .fullName(effectiveFullName.trim())
                .dateOfBirth(effectiveDob)
                .clinicId(effectiveClinicId)
                .build();

        log.info("Received GET patient lookup request for phone='{}', fullName='{}', dob='{}', clinicId='{}'",
                effectivePhone, effectiveFullName, effectiveDob, effectiveClinicId);

        PatientLookupResponse response = patientLookupService.lookupPatient(request);
        return ResponseEntity.ok(ApiResponse.success("Patient found successfully", response));
    }

    /**
     * Patient lookup via HTTP POST with JSON body.
     * Mandatory parameters: phoneNumber, fullName, dateOfBirth.
     *
     * @param pathClinicId optional clinicId from path variable (/api/clinics/{clinicId}/patients/lookup)
     * @param request PatientLookupRequest
     * @return ApiResponse containing PatientLookupResponse
     */
    @PostMapping("/lookup")
    public ResponseEntity<ApiResponse<PatientLookupResponse>> lookupPatientPost(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @Valid @RequestBody PatientLookupRequest request) {

        if (request.getClinicId() == null && pathClinicId != null) {
            request.setClinicId(pathClinicId);
        }

        log.info("Received POST patient lookup request for phone='{}', fullName='{}', dob='{}', clinicId='{}'",
                request.getPhoneNumber(), request.getFullName(), request.getDateOfBirth(), request.getClinicId());

        PatientLookupResponse response = patientLookupService.lookupPatient(request);
        return ResponseEntity.ok(ApiResponse.success("Patient found successfully", response));
    }
}
