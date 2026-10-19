package com.dental.manage.service;

import com.dental.manage.dto.request.ServiceRequest;
import com.dental.manage.dto.response.ServiceResponse;

import java.util.List;

/**
 * Service interface for managing dental services in a multi-tenant clinic architecture.
 */
public interface ServicesService {

    /**
     * Fetches all available and active dental services across all clinics (or default).
     *
     * @return List of active services
     */
    List<ServiceResponse> getActiveServices();

    /**
     * Fetches all available and active dental services for a specific clinic.
     *
     * @param clinicId Clinic ID
     * @return List of active services for that clinic
     */
    List<ServiceResponse> getActiveServices(Long clinicId);

    /**
     * Alias for getActiveServices() to fetch services available and active.
     *
     * @return List of active services
     */
    default List<ServiceResponse> getAvailableActiveServices() {
        return getActiveServices();
    }

    /**
     * Alias for getActiveServices(clinicId) to fetch services available and active in that clinic.
     *
     * @param clinicId Clinic ID
     * @return List of active services for that clinic
     */
    default List<ServiceResponse> getAvailableActiveServices(Long clinicId) {
        return getActiveServices(clinicId);
    }

    /**
     * Fetches all non-deleted services across all clinics.
     *
     * @return List of all services
     */
    List<ServiceResponse> getAllServices();

    /**
     * Fetches all non-deleted services for a specific clinic.
     *
     * @param clinicId Clinic ID
     * @return List of all services for that clinic
     */
    List<ServiceResponse> getAllServices(Long clinicId);

    /**
     * Fetches a single service by ID.
     *
     * @param id Service ID
     * @return Service response
     */
    ServiceResponse getServiceById(Long id);

    /**
     * Fetches a single service by ID and clinicId.
     *
     * @param id Service ID
     * @param clinicId Clinic ID
     * @return Service response
     */
    ServiceResponse getServiceByIdAndClinicId(Long id, Long clinicId);

    /**
     * Creates a new service.
     *
     * @param request Service creation details
     * @return Created service response
     */
    ServiceResponse createService(ServiceRequest request);

    /**
     * Updates an existing service.
     *
     * @param id Service ID
     * @param request Updated service details
     * @return Updated service response
     */
    ServiceResponse updateService(Long id, ServiceRequest request);

    /**
     * Soft-deletes a service by ID.
     *
     * @param id Service ID
     */
    void deleteService(Long id);
}
