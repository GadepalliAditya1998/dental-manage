package com.dental.manage.controller;

import com.dental.manage.dto.request.PatientLookupRequest;
import com.dental.manage.dto.response.PatientLookupResponse;
import com.dental.manage.exception.GlobalExceptionHandler;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.service.PatientLookupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PatientLookupService patientLookupService;

    @InjectMocks
    private PatientController patientController;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private PatientLookupResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(patientController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = PatientLookupResponse.builder()
                .id(100L)
                .clinicId(1L)
                .username("patient_john")
                .fullName("John Doe")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1-555-0199")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .role("PATIENT")
                .isActive(true)
                .createdOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .modifiedOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    @Test
    @DisplayName("GET /api/patients/lookup should return 200 OK when patient is found")
    void lookupPatient_get_validParams_shouldReturn200() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/patients/lookup")
                        .param("phoneNumber", "+1-555-0199")
                        .param("fullName", "John Doe")
                        .param("dateOfBirth", "1990-05-15")
                        .param("clinicId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Patient found successfully")))
                .andExpect(jsonPath("$.data.id", is(100)))
                .andExpect(jsonPath("$.data.clinicId", is(1)))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")))
                .andExpect(jsonPath("$.data.firstName", is("John")))
                .andExpect(jsonPath("$.data.lastName", is("Doe")))
                .andExpect(jsonPath("$.data.phoneNumber", is("+1-555-0199")))
                .andExpect(jsonPath("$.data.dateOfBirth", is("1990-05-15")))
                .andExpect(jsonPath("$.data.role", is("PATIENT")));
    }

    @Test
    @DisplayName("GET /api/patients/lookup with aliases (phone, name, dob) should return 200 OK")
    void lookupPatient_get_aliasParams_shouldReturn200() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/patients/lookup")
                        .param("phone", "+1-555-0199")
                        .param("name", "John Doe")
                        .param("dob", "1990-05-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")));
    }

    @Test
    @DisplayName("GET /api/clinics/{clinicId}/patients/lookup should pass clinicId and return 200 OK")
    void lookupPatient_get_scopedClinicId_shouldReturn200() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/clinics/1/patients/lookup")
                        .param("phoneNumber", "+1-555-0199")
                        .param("fullName", "John Doe")
                        .param("dateOfBirth", "1990-05-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.clinicId", is(1)));
    }

    @Test
    @DisplayName("GET /api/patients/lookup missing phone number should return 400 Bad Request")
    void lookupPatient_get_missingPhone_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/patients/lookup")
                        .param("fullName", "John Doe")
                        .param("dateOfBirth", "1990-05-15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Phone number is mandatory")));
    }

    @Test
    @DisplayName("GET /api/patients/lookup missing full name should return 400 Bad Request")
    void lookupPatient_get_missingFullName_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/patients/lookup")
                        .param("phoneNumber", "+1-555-0199")
                        .param("dateOfBirth", "1990-05-15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Full name is mandatory")));
    }

    @Test
    @DisplayName("GET /api/patients/lookup missing date of birth should return 400 Bad Request")
    void lookupPatient_get_missingDateOfBirth_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/patients/lookup")
                        .param("phoneNumber", "+1-555-0199")
                        .param("fullName", "John Doe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Date of birth is mandatory")));
    }

    @Test
    @DisplayName("GET /api/patients/lookup future date of birth should return 400 Bad Request")
    void lookupPatient_get_futureDateOfBirth_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/patients/lookup")
                        .param("phoneNumber", "+1-555-0199")
                        .param("fullName", "John Doe")
                        .param("dateOfBirth", "2099-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Date of birth must be in the past")));
    }

    @Test
    @DisplayName("GET /api/patients/lookup not found should return 404 Not Found")
    void lookupPatient_get_notFound_shouldReturn404() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class)))
                .thenThrow(new ResourceNotFoundException("Patient not found with the provided phone number, full name, and date of birth"));

        mockMvc.perform(get("/api/patients/lookup")
                        .param("phoneNumber", "+1-555-0199")
                        .param("fullName", "John Doe")
                        .param("dateOfBirth", "1990-05-15"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Patient not found")));
    }

    @Test
    @DisplayName("POST /api/patients/lookup should return 200 OK with valid body")
    void lookupPatient_post_validBody_shouldReturn200() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class))).thenReturn(sampleResponse);

        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("+1-555-0199")
                .fullName("John Doe")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .clinicId(1L)
                .build();

        mockMvc.perform(post("/api/patients/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")))
                .andExpect(jsonPath("$.data.role", is("PATIENT")));
    }

    @Test
    @DisplayName("POST /api/patients/lookup missing fields should return 400 Bad Request")
    void lookupPatient_post_missingFields_shouldReturn400() throws Exception {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("")
                .fullName("")
                .dateOfBirth(null)
                .build();

        mockMvc.perform(post("/api/patients/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/patients/lookup future date of birth should return 400 Bad Request")
    void lookupPatient_post_futureDateOfBirth_shouldReturn400() throws Exception {
        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("+1-555-0199")
                .fullName("John Doe")
                .dateOfBirth(LocalDate.now().plusYears(1))
                .build();

        mockMvc.perform(post("/api/patients/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Date of birth must be in the past")));
    }

    @Test
    @DisplayName("POST /api/patients/lookup not found should return 404 Not Found")
    void lookupPatient_post_notFound_shouldReturn404() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class)))
                .thenThrow(new ResourceNotFoundException("Patient not found with the provided phone number, full name, and date of birth"));

        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("+1-555-0199")
                .fullName("John Doe")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .build();

        mockMvc.perform(post("/api/patients/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Patient not found")));
    }
}
