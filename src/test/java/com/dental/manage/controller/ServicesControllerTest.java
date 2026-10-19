package com.dental.manage.controller;

import com.dental.manage.dto.request.ServiceRequest;
import com.dental.manage.dto.response.ServiceResponse;
import com.dental.manage.exception.GlobalExceptionHandler;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.service.ServicesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ServicesControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ServicesService servicesService;

    @InjectMocks
    private ServicesController servicesController;

    private ServiceResponse serviceResponse1;
    private ServiceResponse serviceResponse2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(servicesController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        serviceResponse1 = ServiceResponse.builder()
                .id(1L)
                .clinicId(1L)
                .name("General checkup")
                .description("Routine comprehensive examination")
                .durationMinutes(30)
                .price(new BigDecimal("60.00"))
                .isActive(true)
                .build();

        serviceResponse2 = ServiceResponse.builder()
                .id(2L)
                .clinicId(1L)
                .name("Teeth scaling")
                .description("Deep ultrasonic cleaning")
                .durationMinutes(60)
                .price(new BigDecimal("120.00"))
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("GET /api/services should fetch active and available services for the clinic")
    void getServices_shouldReturnActiveServicesByDefault() throws Exception {
        when(servicesService.getActiveServices(null)).thenReturn(List.of(serviceResponse1, serviceResponse2));

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id", is(1)))
                .andExpect(jsonPath("$.data[0].clinicId", is(1)))
                .andExpect(jsonPath("$.data[0].name", is("General checkup")))
                .andExpect(jsonPath("$.data[0].durationMinutes", is(30)))
                .andExpect(jsonPath("$.data[0].price", is(60.00)))
                .andExpect(jsonPath("$.data[0].isActive", is(true)))
                .andExpect(jsonPath("$.data[1].id", is(2)))
                .andExpect(jsonPath("$.data[1].name", is("Teeth scaling")));

        verify(servicesService).getActiveServices(null);
    }

    @Test
    @DisplayName("GET /api/services?clinicId=1 should fetch active services for specified clinicId")
    void getServices_withClinicIdQueryParam_shouldReturnClinicServices() throws Exception {
        when(servicesService.getActiveServices(1L)).thenReturn(List.of(serviceResponse1));

        mockMvc.perform(get("/api/services").param("clinicId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].clinicId", is(1)));

        verify(servicesService).getActiveServices(1L);
    }

    @Test
    @DisplayName("GET /api/clinics/1/services should fetch active services for clinic 1 via path")
    void getServices_withClinicIdPathVariable_shouldReturnClinicServices() throws Exception {
        when(servicesService.getActiveServices(1L)).thenReturn(List.of(serviceResponse1, serviceResponse2));

        mockMvc.perform(get("/api/clinics/1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)));

        verify(servicesService).getActiveServices(1L);
    }

    @Test
    @DisplayName("GET /api/services/active should fetch active services for the clinic")
    void getActiveServices_shouldReturnActiveServices() throws Exception {
        when(servicesService.getActiveServices(null)).thenReturn(List.of(serviceResponse1));

        mockMvc.perform(get("/api/services/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("General checkup")));

        verify(servicesService).getActiveServices(null);
    }

    @Test
    @DisplayName("GET /api/clinic/services should also route to services controller")
    void getClinicServices_shouldReturnServices() throws Exception {
        when(servicesService.getActiveServices(null)).thenReturn(List.of(serviceResponse1, serviceResponse2));

        mockMvc.perform(get("/api/clinic/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)));

        verify(servicesService).getActiveServices(null);
    }

    @Test
    @DisplayName("GET /api/services?activeOnly=false should fetch all services")
    void getServices_withActiveOnlyFalse_shouldReturnAllServices() throws Exception {
        when(servicesService.getAllServices(null)).thenReturn(List.of(serviceResponse1, serviceResponse2));

        mockMvc.perform(get("/api/services").param("activeOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)));

        verify(servicesService).getAllServices(null);
    }

    @Test
    @DisplayName("GET /api/services/{id} should return single service")
    void getServiceById_shouldReturnService() throws Exception {
        when(servicesService.getServiceById(1L)).thenReturn(serviceResponse1);

        mockMvc.perform(get("/api/services/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.name", is("General checkup")));

        verify(servicesService).getServiceById(1L);
    }

    @Test
    @DisplayName("GET /api/services/{id}?clinicId=1 should return service scoped to clinic")
    void getServiceById_withClinicId_shouldReturnScopedService() throws Exception {
        when(servicesService.getServiceByIdAndClinicId(1L, 1L)).thenReturn(serviceResponse1);

        mockMvc.perform(get("/api/services/1").param("clinicId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.clinicId", is(1)));

        verify(servicesService).getServiceByIdAndClinicId(1L, 1L);
    }

    @Test
    @DisplayName("GET /api/services/{id} should return 404 when not found")
    void getServiceById_notFound_shouldReturn404() throws Exception {
        when(servicesService.getServiceById(99L))
                .thenThrow(new ResourceNotFoundException("Service not found with id: 99"));

        mockMvc.perform(get("/api/services/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Service not found with id: 99")));
    }

    @Test
    @DisplayName("POST /api/services should create a new service with clinicId")
    void createService_shouldReturnCreated() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .clinicId(1L)
                .name("Root canal")
                .description("Endodontic therapy")
                .durationMinutes(60)
                .price(new BigDecimal("750.00"))
                .isActive(true)
                .build();

        ServiceResponse createdResponse = ServiceResponse.builder()
                .id(3L)
                .clinicId(1L)
                .name("Root canal")
                .description("Endodontic therapy")
                .durationMinutes(60)
                .price(new BigDecimal("750.00"))
                .isActive(true)
                .build();

        when(servicesService.createService(any(ServiceRequest.class))).thenReturn(createdResponse);

        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(3)))
                .andExpect(jsonPath("$.data.clinicId", is(1)))
                .andExpect(jsonPath("$.data.name", is("Root canal")));

        verify(servicesService).createService(any(ServiceRequest.class));
    }

    @Test
    @DisplayName("PUT /api/services/{id} should update service")
    void updateService_shouldReturnUpdated() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .clinicId(1L)
                .name("Updated Checkup")
                .durationMinutes(45)
                .price(new BigDecimal("80.00"))
                .isActive(true)
                .build();

        ServiceResponse updatedResponse = ServiceResponse.builder()
                .id(1L)
                .clinicId(1L)
                .name("Updated Checkup")
                .durationMinutes(45)
                .price(new BigDecimal("80.00"))
                .isActive(true)
                .build();

        when(servicesService.updateService(eq(1L), any(ServiceRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/services/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Updated Checkup")))
                .andExpect(jsonPath("$.data.durationMinutes", is(45)));

        verify(servicesService).updateService(eq(1L), any(ServiceRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/services/{id} should delete service")
    void deleteService_shouldReturnOk() throws Exception {
        doNothing().when(servicesService).deleteService(1L);

        mockMvc.perform(delete("/api/services/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(servicesService).deleteService(1L);
    }
}
