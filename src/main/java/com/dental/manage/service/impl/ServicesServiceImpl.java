package com.dental.manage.service.impl;

import com.dental.manage.dto.request.ServiceRequest;
import com.dental.manage.dto.response.ServiceResponse;
import com.dental.manage.entity.Service;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.mapper.ServiceMapper;
import com.dental.manage.repository.ServiceRepository;
import com.dental.manage.service.ServicesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of ServicesService for managing dental clinic services in a multi-tenant environment.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ServicesServiceImpl implements ServicesService {

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;

    @Override
    public List<ServiceResponse> getActiveServices() {
        log.info("Fetching active and available services across all clinics");
        List<Service> services = serviceRepository.findByIsActiveTrueAndIsDeletedFalse();
        log.debug("Found {} active services", services.size());
        return serviceMapper.toResponseList(services);
    }

    @Override
    public List<ServiceResponse> getActiveServices(Long clinicId) {
        if (clinicId == null) {
            return getActiveServices();
        }
        log.info("Fetching active and available services for clinicId: {}", clinicId);
        List<Service> services = serviceRepository.findByClinicIdAndIsActiveTrueAndIsDeletedFalse(clinicId);
        log.debug("Found {} active services for clinicId: {}", services.size(), clinicId);
        return serviceMapper.toResponseList(services);
    }

    @Override
    public List<ServiceResponse> getAllServices() {
        log.info("Fetching all non-deleted services across all clinics");
        List<Service> services = serviceRepository.findAll().stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .toList();
        return serviceMapper.toResponseList(services);
    }

    @Override
    public List<ServiceResponse> getAllServices(Long clinicId) {
        if (clinicId == null) {
            return getAllServices();
        }
        log.info("Fetching all non-deleted services for clinicId: {}", clinicId);
        List<Service> services = serviceRepository.findByClinicIdAndIsDeletedFalse(clinicId);
        return serviceMapper.toResponseList(services);
    }

    @Override
    public ServiceResponse getServiceById(Long id) {
        log.info("Fetching service with id: {}", id);
        Service service = serviceRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));
        return serviceMapper.toResponse(service);
    }

    @Override
    public ServiceResponse getServiceByIdAndClinicId(Long id, Long clinicId) {
        if (clinicId == null) {
            return getServiceById(id);
        }
        log.info("Fetching service with id: {} for clinicId: {}", id, clinicId);
        Service service = serviceRepository.findByIdAndClinicIdAndIsDeletedFalse(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id + " for clinic: " + clinicId));
        return serviceMapper.toResponse(service);
    }

    @Override
    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        log.info("Creating new clinic service: {} for clinicId: {}", request.getName(), request.getClinicId());
        Service service = serviceMapper.toEntity(request);
        if (service.getIsActive() == null) {
            service.setIsActive(true);
        }
        if (service.getClinicId() == null) {
            service.setClinicId(1L); // default to clinic 1 if not specified
        }
        Service saved = serviceRepository.save(service);
        log.info("Created clinic service with id: {} for clinicId: {}", saved.getId(), saved.getClinicId());
        return serviceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ServiceResponse updateService(Long id, ServiceRequest request) {
        log.info("Updating clinic service with id: {}", id);
        Service service = serviceRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));

        serviceMapper.updateEntityFromRequest(request, service);
        Service updated = serviceRepository.save(service);
        log.info("Updated clinic service with id: {}", updated.getId());
        return serviceMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteService(Long id) {
        log.info("Soft deleting clinic service with id: {}", id);
        Service service = serviceRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));
        service.setIsDeleted(true);
        serviceRepository.save(service);
        log.info("Soft deleted clinic service with id: {}", id);
    }
}
