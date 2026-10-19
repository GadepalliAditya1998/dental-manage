package com.dental.manage.controller;

import com.dental.manage.dto.request.ServiceRequest;
import com.dental.manage.dto.response.ApiResponse;
import com.dental.manage.dto.response.ServiceResponse;
import com.dental.manage.service.ServicesService;
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
 * Controller for clinic dental services with multi-tenant architecture support.
 * Uses ServicesService to fetch services available and active in that clinic.
 */
@RestController
@RequestMapping({"/api/services", "/api/clinic/services", "/api/clinics/{clinicId}/services"})
@RequiredArgsConstructor
@Slf4j
public class ServicesController {

    private final ServicesService servicesService;

    /**
     * Fetches the list of services available and active in that clinic.
     * Defaults to active services only, but can retrieve all services via activeOnly=false.
     * Supports multi-tenant scoping via path variable or query parameter (e.g. ?clinicId=1).
     *
     * @param pathClinicId optional clinicId from path variable (/api/clinics/{clinicId}/services)
     * @param queryClinicId optional clinicId from query param (?clinicId=1)
     * @param activeOnly whether to filter only active services (defaults to true)
     * @return ApiResponse containing list of ServiceResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getServices(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId,
            @RequestParam(name = "activeOnly", required = false, defaultValue = "true") boolean activeOnly) {
        Long clinicId = pathClinicId != null ? pathClinicId : queryClinicId;
        log.info("Request received to fetch clinic services (clinicId={}, activeOnly={})", clinicId, activeOnly);
        List<ServiceResponse> services = activeOnly
                ? servicesService.getActiveServices(clinicId)
                : servicesService.getAllServices(clinicId);
        return ResponseEntity.ok(ApiResponse.success("Services fetched successfully", services));
    }

    /**
     * Dedicated endpoint to fetch active and available services in that clinic.
     *
     * @param pathClinicId optional clinicId from path variable
     * @param queryClinicId optional clinicId from query param
     * @return ApiResponse containing list of active ServiceResponse
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getActiveServices(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId) {
        Long clinicId = pathClinicId != null ? pathClinicId : queryClinicId;
        log.info("Request received to fetch active clinic services (clinicId={})", clinicId);
        List<ServiceResponse> services = servicesService.getActiveServices(clinicId);
        return ResponseEntity.ok(ApiResponse.success("Active services fetched successfully", services));
    }

    /**
     * Fetches a single service by ID (and optional clinicId scoping).
     *
     * @param id Service ID
     * @param pathClinicId optional clinicId from path variable
     * @param queryClinicId optional clinicId from query param
     * @return ApiResponse containing the service details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceResponse>> getServiceById(
            @PathVariable Long id,
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId) {
        Long clinicId = pathClinicId != null ? pathClinicId : queryClinicId;
        log.info("Request received to fetch service with id: {} (clinicId={})", id, clinicId);
        ServiceResponse service = (clinicId != null)
                ? servicesService.getServiceByIdAndClinicId(id, clinicId)
                : servicesService.getServiceById(id);
        return ResponseEntity.ok(ApiResponse.success("Service retrieved successfully", service));
    }

    /**
     * Creates a new service for the clinic.
     *
     * @param pathClinicId optional clinicId from path variable
     * @param request Service creation details
     * @return ApiResponse containing the created service
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ServiceResponse>> createService(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @Valid @RequestBody ServiceRequest request) {
        if (pathClinicId != null && request.getClinicId() == null) {
            request.setClinicId(pathClinicId);
        }
        log.info("Request received to create service: {} for clinicId: {}", request.getName(), request.getClinicId());
        ServiceResponse created = servicesService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service created successfully", created));
    }

    /**
     * Updates an existing service.
     *
     * @param id Service ID
     * @param request Updated service details
     * @return ApiResponse containing the updated service
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceResponse>> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request) {
        log.info("Request received to update service with id: {}", id);
        ServiceResponse updated = servicesService.updateService(id, request);
        return ResponseEntity.ok(ApiResponse.success("Service updated successfully", updated));
    }

    /**
     * Soft-deletes a service by ID.
     *
     * @param id Service ID
     * @return ApiResponse indicating deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable Long id) {
        log.info("Request received to delete service with id: {}", id);
        servicesService.deleteService(id);
        return ResponseEntity.ok(ApiResponse.success("Service deleted successfully", null));
    }
}
