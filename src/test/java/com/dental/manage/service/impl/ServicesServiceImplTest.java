package com.dental.manage.service.impl;

import com.dental.manage.dto.request.ServiceRequest;
import com.dental.manage.dto.response.ServiceResponse;
import com.dental.manage.entity.Service;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.mapper.ServiceMapper;
import com.dental.manage.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicesServiceImplTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ServiceMapper serviceMapper;

    @InjectMocks
    private ServicesServiceImpl servicesService;

    private Service service1;
    private Service service2;
    private ServiceResponse response1;
    private ServiceResponse response2;

    @BeforeEach
    void setUp() {
        service1 = Service.builder()
                .clinicId(1L)
                .name("General checkup")
                .description("Routine dental exam")
                .durationMinutes(30)
                .price(new BigDecimal("60.00"))
                .isActive(true)
                .build();
        service1.setId(1L);

        service2 = Service.builder()
                .clinicId(1L)
                .name("Teeth scaling")
                .description("Ultrasonic deep cleaning")
                .durationMinutes(60)
                .price(new BigDecimal("120.00"))
                .isActive(true)
                .build();
        service2.setId(2L);

        response1 = ServiceResponse.builder()
                .id(1L)
                .clinicId(1L)
                .name("General checkup")
                .description("Routine dental exam")
                .durationMinutes(30)
                .price(new BigDecimal("60.00"))
                .isActive(true)
                .build();

        response2 = ServiceResponse.builder()
                .id(2L)
                .clinicId(1L)
                .name("Teeth scaling")
                .description("Ultrasonic deep cleaning")
                .durationMinutes(60)
                .price(new BigDecimal("120.00"))
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("getActiveServices should return only active and non-deleted services across all clinics")
    void getActiveServices_shouldReturnActiveServices() {
        when(serviceRepository.findByIsActiveTrueAndIsDeletedFalse()).thenReturn(List.of(service1, service2));
        when(serviceMapper.toResponseList(List.of(service1, service2))).thenReturn(List.of(response1, response2));

        List<ServiceResponse> result = servicesService.getActiveServices();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("General checkup");
        assertThat(result.get(1).getName()).isEqualTo("Teeth scaling");
        verify(serviceRepository).findByIsActiveTrueAndIsDeletedFalse();
    }

    @Test
    @DisplayName("getActiveServices(clinicId) should return only active services for specific clinic")
    void getActiveServicesByClinicId_shouldReturnActiveServicesForClinic() {
        when(serviceRepository.findByClinicIdAndIsActiveTrueAndIsDeletedFalse(1L)).thenReturn(List.of(service1, service2));
        when(serviceMapper.toResponseList(List.of(service1, service2))).thenReturn(List.of(response1, response2));

        List<ServiceResponse> result = servicesService.getActiveServices(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getClinicId()).isEqualTo(1L);
        verify(serviceRepository).findByClinicIdAndIsActiveTrueAndIsDeletedFalse(1L);
    }

    @Test
    @DisplayName("getAvailableActiveServices alias should also return active services")
    void getAvailableActiveServices_shouldReturnActiveServices() {
        when(serviceRepository.findByIsActiveTrueAndIsDeletedFalse()).thenReturn(List.of(service1));
        when(serviceMapper.toResponseList(List.of(service1))).thenReturn(List.of(response1));

        List<ServiceResponse> result = servicesService.getAvailableActiveServices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("General checkup");
    }

    @Test
    @DisplayName("getServiceById should return service when found")
    void getServiceById_shouldReturnService() {
        when(serviceRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(service1));
        when(serviceMapper.toResponse(service1)).thenReturn(response1);

        ServiceResponse result = servicesService.getServiceById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("General checkup");
    }

    @Test
    @DisplayName("getServiceByIdAndClinicId should return service when found for clinic")
    void getServiceByIdAndClinicId_shouldReturnService() {
        when(serviceRepository.findByIdAndClinicIdAndIsDeletedFalse(1L, 1L)).thenReturn(Optional.of(service1));
        when(serviceMapper.toResponse(service1)).thenReturn(response1);

        ServiceResponse result = servicesService.getServiceByIdAndClinicId(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getClinicId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getServiceById should throw ResourceNotFoundException when service does not exist")
    void getServiceById_shouldThrowNotFoundException() {
        when(serviceRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicesService.getServiceById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("createService should save with clinicId and return created service")
    void createService_shouldCreateAndReturnService() {
        ServiceRequest request = ServiceRequest.builder()
                .clinicId(1L)
                .name("Root canal")
                .description("Endodontic therapy")
                .durationMinutes(60)
                .price(new BigDecimal("750.00"))
                .isActive(true)
                .build();

        when(serviceMapper.toEntity(request)).thenReturn(service1);
        when(serviceRepository.save(any(Service.class))).thenReturn(service1);
        when(serviceMapper.toResponse(service1)).thenReturn(response1);

        ServiceResponse result = servicesService.createService(request);

        assertThat(result).isNotNull();
        verify(serviceRepository).save(any(Service.class));
    }

    @Test
    @DisplayName("deleteService should soft delete service")
    void deleteService_shouldSoftDelete() {
        when(serviceRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(service1));

        servicesService.deleteService(1L);

        assertThat(service1.getIsDeleted()).isTrue();
        verify(serviceRepository).save(service1);
    }
}
