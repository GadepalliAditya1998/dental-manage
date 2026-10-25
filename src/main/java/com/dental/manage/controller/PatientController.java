package com.dental.manage.controller;

import com.dental.manage.context.ClinicRequestContext;
import com.dental.manage.dto.request.PatientLookupRequest;
import com.dental.manage.dto.request.PatientRequest;
import com.dental.manage.dto.response.ApiResponse;
import com.dental.manage.dto.response.PatientLookupResponse;
import com.dental.manage.dto.response.PatientResponse;
import com.dental.manage.dto.response.PatientUpcomingAppointmentResponse;
import com.dental.manage.service.PatientLookupService;
import com.dental.manage.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for patient operations including patient lookup and creation.
 * Supports patient creation with an 8-character random alphanumeric password and username equal to email.
 * Supports patient lookup by mobile number and date of birth, or by unique string patientId.
 */
@RestController
@RequestMapping({"/api/patients", "/api/patient", "/api/clinics/{clinicId}/patients"})
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientLookupService patientLookupService;
    private final PatientService patientService;

    /**
     * Retrieves all non-deleted patients, optionally filtered by clinic and search query.
     *
     * @param pathClinicId optional clinicId from path variable (/api/clinics/{clinicId}/patients)
     * @param queryClinicId optional clinicId from query parameter (?clinicId=1)
     * @param search optional search keyword matching name, phone, email, or patientId
     * @return ApiResponse containing list of PatientResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getPatients(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId,
            @RequestParam(name = "search", required = false) String search) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        log.info("Request received to fetch patients (clinicId={}, search='{}')", clinicId, search);

        List<PatientResponse> patients = patientService.getPatients(clinicId, search);
        return ResponseEntity.ok(ApiResponse.success("Patients fetched successfully", patients));
    }

    /**
     * Retrieves a patient by ID.
     *
     * @param id patient ID
     * @param pathClinicId optional clinicId from path variable
     * @param queryClinicId optional clinicId from query parameter
     * @return ApiResponse containing PatientResponse
     */
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(
            @PathVariable(name = "id") Long id,
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        log.info("Request received to fetch patient with id: {} (clinicId={})", id, clinicId);

        PatientResponse response = patientService.getPatientById(id, clinicId);
        return ResponseEntity.ok(ApiResponse.success("Patient retrieved successfully", response));
    }

    /**
     * Updates an existing patient record.
     *
     * @param id patient ID
     * @param pathClinicId optional clinicId from path variable
     * @param queryClinicId optional clinicId from query parameter
     * @param request PatientRequest with updated fields
     * @return ApiResponse containing updated PatientResponse
     */
    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @PathVariable(name = "id") Long id,
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId,
            @RequestBody PatientRequest request) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        log.info("Request received to update patient with id: {} (clinicId={})", id, clinicId);

        PatientResponse response = patientService.updatePatient(id, request, clinicId);
        return ResponseEntity.ok(ApiResponse.success("Patient updated successfully", response));
    }

    /**
     * Soft deletes a patient record and associated user account.
     *
     * @param id patient ID
     * @param pathClinicId optional clinicId from path variable
     * @param queryClinicId optional clinicId from query parameter
     * @return ApiResponse with success message
     */
    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<Void>> deletePatient(
            @PathVariable(name = "id") Long id,
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId) {

        Long clinicId = pathClinicId != null ? pathClinicId : queryClinicId;
        log.info("Request received to delete patient with id: {} (clinicId={})", id, clinicId);

        patientService.deletePatient(id, clinicId);
        return ResponseEntity.ok(ApiResponse.success("Patient deleted successfully", null));
    }

    /**
     * Retrieves all upcoming appointments for a given patient.
     *
     * @param id patient ID
     * @return ApiResponse containing list of upcoming appointments
     */
    @GetMapping("/{id:[0-9]+}/appointments/upcoming")
    public ResponseEntity<ApiResponse<List<PatientUpcomingAppointmentResponse>>> getUpcomingAppointments(
            @PathVariable(name = "id") Long id) {

        log.info("Request received to fetch upcoming appointments for patient id: {}", id);

        List<PatientUpcomingAppointmentResponse> response = patientService.getUpcomingAppointments(id);
        return ResponseEntity.ok(ApiResponse.success("Upcoming appointments fetched successfully", response));
    }

    /**
     * Creates a new patient record with a secure 8-character random alphanumeric password,
     * username set to email, and associates it with the clinic.
     *
     * @param pathClinicId optional clinicId from path variable (/api/clinics/{clinicId}/patients)
     * @param request PatientRequest containing patient name, email, phone number, date of birth
     * @return ApiResponse containing the created PatientResponse
     */
    @PostMapping({"", "/create"})
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @Valid @RequestBody PatientRequest request) {

        Long clinicId = pathClinicId != null ? pathClinicId : ClinicRequestContext.getClinicId();
        if (clinicId != null && request.getClinicId() == null) {
            request.setClinicId(clinicId);
        }

        log.info("Request received to create patient: name='{}', email='{}', phone='{}', dob='{}', clinicId='{}'",
                request.getEffectiveFullName(), request.getEmail(), request.getPhoneNumber(),
                request.getDateOfBirth(), request.getClinicId());

        PatientResponse response = patientService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient created successfully", response));
    }

    /**
     * Patient lookup via HTTP GET with query parameters.
     * Supports search with:
     * 1) Unique string patientId (e.g. PAT-1-0192B...)
     * OR
     * 2) Mobile number and date of birth.
     *
     * @param pathClinicId optional clinicId from path variable (/api/clinics/{clinicId}/patients/lookup)
     * @param patientId optional unique string patient identifier
     * @param id alias for patientId
     * @param phoneNumber patient phone/mobile number
     * @param phone alias for phoneNumber
     * @param mobileNumber alias for phoneNumber
     * @param mobile alias for phoneNumber
     * @param fullName optional patient full name
     * @param name alias for fullName
     * @param dateOfBirth patient date of birth (YYYY-MM-DD)
     * @param dob alias for dateOfBirth
     * @param queryClinicId optional clinicId from query parameter
     * @return ApiResponse containing PatientLookupResponse
     */
    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<PatientLookupResponse>> lookupPatient(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "patientId", required = false) String patientId,
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "mobileNumber", required = false) String mobileNumber,
            @RequestParam(name = "mobile", required = false) String mobile,
            @RequestParam(name = "fullName", required = false) String fullName,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "dateOfBirth", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam(name = "dob", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId) {

        String effectivePatientId = (patientId != null && !patientId.isBlank()) ? patientId : id;
        String effectivePhone = (phoneNumber != null && !phoneNumber.isBlank()) ? phoneNumber
                : (phone != null && !phone.isBlank()) ? phone
                : (mobileNumber != null && !mobileNumber.isBlank()) ? mobileNumber : mobile;
        String effectiveFullName = (fullName != null && !fullName.isBlank()) ? fullName : name;
        LocalDate effectiveDob = dateOfBirth != null ? dateOfBirth : dob;
        Long effectiveClinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());

        PatientLookupRequest request = PatientLookupRequest.builder()
                .patientId(effectivePatientId != null ? effectivePatientId.trim() : null)
                .phoneNumber(effectivePhone != null ? effectivePhone.trim() : null)
                .fullName(effectiveFullName != null ? effectiveFullName.trim() : null)
                .dateOfBirth(effectiveDob)
                .clinicId(effectiveClinicId)
                .build();

        log.info("Received GET patient lookup request for patientId='{}', phone='{}', fullName='{}', dob='{}', clinicId='{}'",
                request.getPatientId(), request.getPhoneNumber(), request.getFullName(), request.getDateOfBirth(), request.getClinicId());

        PatientLookupResponse response = patientLookupService.lookupPatient(request);
        return ResponseEntity.ok(ApiResponse.success("Patient found successfully", response));
    }

    /**
     * Patient lookup via HTTP POST with JSON body.
     * Supports search with unique string patientId OR (mobile number + date of birth).
     *
     * @param pathClinicId optional clinicId from path variable (/api/clinics/{clinicId}/patients/lookup)
     * @param request PatientLookupRequest containing patientId or (phoneNumber + dateOfBirth)
     * @return ApiResponse containing PatientLookupResponse
     */
    @PostMapping("/lookup")
    public ResponseEntity<ApiResponse<PatientLookupResponse>> lookupPatientPost(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @Valid @RequestBody PatientLookupRequest request) {

        if (request.getClinicId() == null) {
            Long effectiveClinicId = pathClinicId != null ? pathClinicId : ClinicRequestContext.getClinicId();
            if (effectiveClinicId != null) {
                request.setClinicId(effectiveClinicId);
            }
        }

        log.info("Received POST patient lookup request for patientId='{}', phone='{}', fullName='{}', dob='{}', clinicId='{}'",
                request.getPatientId(), request.getPhoneNumber(), request.getFullName(), request.getDateOfBirth(), request.getClinicId());

        PatientLookupResponse response = patientLookupService.lookupPatient(request);
        return ResponseEntity.ok(ApiResponse.success("Patient found successfully", response));
    }
}
