package com.dental.manage.controller;

import com.dental.manage.dto.request.DoctorAvailabilityRequest;
import com.dental.manage.dto.request.DoctorRequest;
import com.dental.manage.dto.response.DoctorAvailabilityResponse;
import com.dental.manage.dto.response.DoctorResponse;
import com.dental.manage.exception.GlobalExceptionHandler;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.service.DoctorManagementService;
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

import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DoctorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DoctorManagementService doctorManagementService;

    @InjectMocks
    private DoctorController doctorController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private DoctorResponse sampleDoctor;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders.standaloneSetup(doctorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleDoctor = DoctorResponse.builder()
                .id(1L)
                .clinicId(1L)
                .firstName("Jane")
                .lastName("Smith")
                .specialization("Orthodontics")
                .email("dr.jane@clinic.com")
                .phoneNumber("+1234567890")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("GET /api/doctors should return list of doctors")
    void getDoctors_shouldReturnSuccess() throws Exception {
        when(doctorManagementService.getDoctors(eq(1L), eq(null), eq(false)))
                .thenReturn(List.of(sampleDoctor));

        mockMvc.perform(get("/api/doctors")
                        .param("clinicId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].firstName", is("Jane")))
                .andExpect(jsonPath("$.data[0].lastName", is("Smith")));
    }

    @Test
    @DisplayName("GET /api/doctors/{id} should return doctor details")
    void getDoctorById_shouldReturnSuccess() throws Exception {
        when(doctorManagementService.getDoctorById(eq(1L), eq(1L))).thenReturn(sampleDoctor);

        mockMvc.perform(get("/api/doctors/1")
                        .param("clinicId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.specialization", is("Orthodontics")));
    }

    @Test
    @DisplayName("GET /api/doctors/{id} should return 404 when not found")
    void getDoctorById_notFound_shouldReturn404() throws Exception {
        when(doctorManagementService.getDoctorById(eq(99L), eq(1L)))
                .thenThrow(new ResourceNotFoundException("Doctor not found with id: 99"));

        mockMvc.perform(get("/api/doctors/99")
                        .param("clinicId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/doctors should create doctor and return 201 Created")
    void createDoctor_shouldReturnCreated() throws Exception {
        DoctorRequest request = DoctorRequest.builder()
                .clinicId(1L)
                .firstName("Jane")
                .lastName("Smith")
                .specialization("Orthodontics")
                .email("dr.jane@clinic.com")
                .build();

        when(doctorManagementService.createDoctor(any(DoctorRequest.class), eq(1L)))
                .thenReturn(sampleDoctor);

        mockMvc.perform(post("/api/doctors")
                        .param("clinicId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.lastName", is("Smith")));
    }

    @Test
    @DisplayName("PUT /api/doctors/{id} should update doctor")
    void updateDoctor_shouldReturnSuccess() throws Exception {
        DoctorRequest request = DoctorRequest.builder()
                .clinicId(1L)
                .firstName("Jane")
                .lastName("Smith-Doe")
                .build();

        DoctorResponse updated = DoctorResponse.builder()
                .id(1L)
                .clinicId(1L)
                .firstName("Jane")
                .lastName("Smith-Doe")
                .build();

        when(doctorManagementService.updateDoctor(eq(1L), any(DoctorRequest.class), eq(1L)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/doctors/1")
                        .param("clinicId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.lastName", is("Smith-Doe")));
    }

    @Test
    @DisplayName("DELETE /api/doctors/{id} should delete doctor")
    void deleteDoctor_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/doctors/1")
                        .param("clinicId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(doctorManagementService).deleteDoctor(eq(1L), eq(1L));
    }

    @Test
    @DisplayName("GET /api/doctors/{id}/availability should return availability schedule")
    void getDoctorAvailability_shouldReturnSuccess() throws Exception {
        DoctorAvailabilityResponse slot = DoctorAvailabilityResponse.builder()
                .id(1L)
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .isActive(true)
                .build();

        when(doctorManagementService.getDoctorAvailability(eq(1L), eq(1L)))
                .thenReturn(List.of(slot));

        mockMvc.perform(get("/api/doctors/1/availability")
                        .param("clinicId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].dayOfWeek", is("MONDAY")));
    }
}
