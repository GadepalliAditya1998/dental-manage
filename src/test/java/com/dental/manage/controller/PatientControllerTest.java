package com.dental.manage.controller;

import com.dental.manage.dto.request.PatientLookupRequest;
import com.dental.manage.dto.request.PatientRequest;
import com.dental.manage.dto.response.PatientLookupResponse;
import com.dental.manage.dto.response.PatientResponse;
import com.dental.manage.exception.GlobalExceptionHandler;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.service.PatientLookupService;
import com.dental.manage.service.PatientService;
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

import com.dental.manage.dto.response.PatientUpcomingAppointmentResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PatientLookupService patientLookupService;

    @Mock
    private PatientService patientService;

    @InjectMocks
    private PatientController patientController;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private PatientLookupResponse sampleLookupResponse;
    private PatientResponse sampleCreatedPatientResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(patientController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleLookupResponse = PatientLookupResponse.builder()
                .id(100L)
                .patientId("PAT-1-0192B6E9F8A3B4C1")
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

        sampleCreatedPatientResponse = PatientResponse.builder()
                .id(1L)
                .patientId("PAT-1-0192B6E9F8A3B4C1")
                .clinicId(1L)
                .firstName("John")
                .lastName("Doe")
                .fullName("John Doe")
                .username("john.doe@example.com")
                .password("aB3dE7k9")
                .email("john.doe@example.com")
                .phoneNumber("+1-555-0199")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .isNewLead(true)
                .createdOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .modifiedOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    // =========================================================================
    // Create Patient API Tests
    // =========================================================================

    @Test
    @DisplayName("POST /api/patients should return 201 Created and return patient details with password & username")
    void createPatient_validBody_shouldReturn201() throws Exception {
        when(patientService.createPatient(any(PatientRequest.class))).thenReturn(sampleCreatedPatientResponse);

        String jsonPayload = """
                {
                    "name": "John Doe",
                    "email": "john.doe@example.com",
                    "phoneNumber": "+1-555-0199",
                    "dateOfBirth": "1990-05-15"
                }
                """;

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Patient created successfully")))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.username", is("john.doe@example.com")))
                .andExpect(jsonPath("$.data.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.data.password", is("aB3dE7k9")))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")))
                .andExpect(jsonPath("$.data.phoneNumber", is("+1-555-0199")))
                .andExpect(jsonPath("$.data.dateOfBirth", is("1990-05-15")));

        verify(patientService).createPatient(any(PatientRequest.class));
    }

    @Test
    @DisplayName("POST /api/patients with aliases (phone, dateofbirth, patientName) should return 201 Created")
    void createPatient_withAliases_shouldReturn201() throws Exception {
        when(patientService.createPatient(any(PatientRequest.class))).thenReturn(sampleCreatedPatientResponse);

        String jsonPayload = """
                {
                    "patientName": "John Doe",
                    "email": "john.doe@example.com",
                    "phone": "+1-555-0199",
                    "dateofbirth": "1990-05-15"
                }
                """;

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("john.doe@example.com")))
                .andExpect(jsonPath("$.data.password", is("aB3dE7k9")));
    }

    @Test
    @DisplayName("POST /api/clinics/{clinicId}/patients should inherit clinicId and return 201 Created")
    void createPatient_withClinicIdPath_shouldReturn201() throws Exception {
        when(patientService.createPatient(any(PatientRequest.class))).thenReturn(sampleCreatedPatientResponse);

        String jsonPayload = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@example.com",
                    "phoneNumber": "+1-555-0199",
                    "dateOfBirth": "1990-05-15"
                }
                """;

        mockMvc.perform(post("/api/clinics/1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)));
    }

    @Test
    @DisplayName("POST /api/patients missing email should return 400 Bad Request")
    void createPatient_missingEmail_shouldReturn400() throws Exception {
        String jsonPayload = """
                {
                    "name": "John Doe",
                    "phoneNumber": "+1-555-0199",
                    "dateOfBirth": "1990-05-15"
                }
                """;

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Email is required")));
    }

    @Test
    @DisplayName("POST /api/patients invalid email should return 400 Bad Request")
    void createPatient_invalidEmail_shouldReturn400() throws Exception {
        String jsonPayload = """
                {
                    "name": "John Doe",
                    "email": "not-an-email",
                    "phoneNumber": "+1-555-0199",
                    "dateOfBirth": "1990-05-15"
                }
                """;

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid email format")));
    }

    @Test
    @DisplayName("POST /api/patients missing phone should return 400 Bad Request")
    void createPatient_missingPhone_shouldReturn400() throws Exception {
        String jsonPayload = """
                {
                    "name": "John Doe",
                    "email": "john.doe@example.com",
                    "dateOfBirth": "1990-05-15"
                }
                """;

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Phone number is required")));
    }

    @Test
    @DisplayName("POST /api/patients missing date of birth should return 400 Bad Request")
    void createPatient_missingDob_shouldReturn400() throws Exception {
        String jsonPayload = """
                {
                    "name": "John Doe",
                    "email": "john.doe@example.com",
                    "phoneNumber": "+1-555-0199"
                }
                """;

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Date of birth is required")));
    }

    @Test
    @DisplayName("POST /api/patients future date of birth should return 400 Bad Request")
    void createPatient_futureDob_shouldReturn400() throws Exception {
        String jsonPayload = """
                {
                    "name": "John Doe",
                    "email": "john.doe@example.com",
                    "phoneNumber": "+1-555-0199",
                    "dateOfBirth": "2099-01-01"
                }
                """;

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Date of birth must be in the past")));
    }

    @Test
    @DisplayName("POST /api/patients duplicate email/username should return 400 Bad Request")
    void createPatient_duplicateEmail_shouldReturn400() throws Exception {
        when(patientService.createPatient(any(PatientRequest.class)))
                .thenThrow(new IllegalArgumentException("A user with username/email 'john.doe@example.com' already exists"));

        String jsonPayload = """
                {
                    "name": "John Doe",
                    "email": "john.doe@example.com",
                    "phoneNumber": "+1-555-0199",
                    "dateOfBirth": "1990-05-15"
                }
                """;

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("A user with username/email 'john.doe@example.com' already exists")));
    }

    // =========================================================================
    // Patient Lookup API Tests (PatientId OR Phone + DOB)
    // =========================================================================

    @Test
    @DisplayName("GET /api/patients/lookup with unique string patientId should return 200 OK")
    void lookupPatient_get_byPatientId_shouldReturn200() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class))).thenReturn(sampleLookupResponse);

        mockMvc.perform(get("/api/patients/lookup")
                        .param("patientId", "PAT-1-0192B6E9F8A3B4C1")
                        .param("clinicId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Patient found successfully")))
                .andExpect(jsonPath("$.data.id", is(100)))
                .andExpect(jsonPath("$.data.patientId", is("PAT-1-0192B6E9F8A3B4C1")))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")));
    }

    @Test
    @DisplayName("GET /api/patients/lookup with id alias should return 200 OK")
    void lookupPatient_get_byIdAlias_shouldReturn200() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class))).thenReturn(sampleLookupResponse);

        mockMvc.perform(get("/api/patients/lookup")
                        .param("id", "PAT-1-0192B6E9F8A3B4C1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.patientId", is("PAT-1-0192B6E9F8A3B4C1")));
    }

    @Test
    @DisplayName("GET /api/patients/lookup with phone and date of birth (no fullName) should return 200 OK")
    void lookupPatient_get_phoneAndDob_noFullName_shouldReturn200() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class))).thenReturn(sampleLookupResponse);

        mockMvc.perform(get("/api/patients/lookup")
                        .param("phoneNumber", "+1-555-0199")
                        .param("dateOfBirth", "1990-05-15")
                        .param("clinicId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Patient found successfully")))
                .andExpect(jsonPath("$.data.id", is(100)))
                .andExpect(jsonPath("$.data.patientId", is("PAT-1-0192B6E9F8A3B4C1")))
                .andExpect(jsonPath("$.data.clinicId", is(1)))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")))
                .andExpect(jsonPath("$.data.firstName", is("John")))
                .andExpect(jsonPath("$.data.lastName", is("Doe")))
                .andExpect(jsonPath("$.data.phoneNumber", is("+1-555-0199")))
                .andExpect(jsonPath("$.data.dateOfBirth", is("1990-05-15")))
                .andExpect(jsonPath("$.data.role", is("PATIENT")));
    }

    @Test
    @DisplayName("GET /api/patients/lookup with aliases (mobileNumber, dob) should return 200 OK")
    void lookupPatient_get_aliasParams_shouldReturn200() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class))).thenReturn(sampleLookupResponse);

        mockMvc.perform(get("/api/patients/lookup")
                        .param("mobileNumber", "+1-555-0199")
                        .param("dob", "1990-05-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")));
    }

    @Test
    @DisplayName("GET /api/clinics/{clinicId}/patients/lookup should pass clinicId and return 200 OK")
    void lookupPatient_get_scopedClinicId_shouldReturn200() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class))).thenReturn(sampleLookupResponse);

        mockMvc.perform(get("/api/clinics/1/patients/lookup")
                        .param("phoneNumber", "+1-555-0199")
                        .param("dateOfBirth", "1990-05-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.clinicId", is(1)));
    }

    @Test
    @DisplayName("GET /api/patients/lookup missing phone number when patientId is absent should return 400 Bad Request")
    void lookupPatient_get_missingPhone_shouldReturn400() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class)))
                .thenThrow(new IllegalArgumentException("Phone number is mandatory"));

        mockMvc.perform(get("/api/patients/lookup")
                        .param("fullName", "John Doe")
                        .param("dateOfBirth", "1990-05-15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Phone number is mandatory")));
    }

    @Test
    @DisplayName("GET /api/patients/lookup missing date of birth when patientId is absent should return 400 Bad Request")
    void lookupPatient_get_missingDateOfBirth_shouldReturn400() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class)))
                .thenThrow(new IllegalArgumentException("Date of birth is mandatory"));

        mockMvc.perform(get("/api/patients/lookup")
                        .param("phoneNumber", "+1-555-0199")
                        .param("fullName", "John Doe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Date of birth is mandatory")));
    }

    @Test
    @DisplayName("GET /api/patients/lookup missing both patientId and (phone, DOB) should return 400 Bad Request")
    void lookupPatient_get_missingAllCriteria_shouldReturn400() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class)))
                .thenThrow(new IllegalArgumentException("Either patientId or both mobile number and date of birth must be provided"));

        mockMvc.perform(get("/api/patients/lookup")
                        .param("fullName", "John Doe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Either patientId or both mobile number and date of birth must be provided")));
    }

    @Test
    @DisplayName("GET /api/patients/lookup future date of birth should return 400 Bad Request")
    void lookupPatient_get_futureDateOfBirth_shouldReturn400() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class)))
                .thenThrow(new IllegalArgumentException("Date of birth must be in the past"));

        mockMvc.perform(get("/api/patients/lookup")
                        .param("phoneNumber", "+1-555-0199")
                        .param("dateOfBirth", "2099-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Date of birth must be in the past")));
    }

    @Test
    @DisplayName("GET /api/patients/lookup not found should return 404 Not Found")
    void lookupPatient_get_notFound_shouldReturn404() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class)))
                .thenThrow(new ResourceNotFoundException("Patient not found with the provided mobile number and date of birth"));

        mockMvc.perform(get("/api/patients/lookup")
                        .param("phoneNumber", "+1-555-0199")
                        .param("dateOfBirth", "1990-05-15"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Patient not found")));
    }

    @Test
    @DisplayName("POST /api/patients/lookup should return 200 OK with valid patientId body")
    void lookupPatient_post_byPatientId_shouldReturn200() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class))).thenReturn(sampleLookupResponse);

        PatientLookupRequest request = PatientLookupRequest.builder()
                .patientId("PAT-1-0192B6E9F8A3B4C1")
                .clinicId(1L)
                .build();

        mockMvc.perform(post("/api/patients/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.patientId", is("PAT-1-0192B6E9F8A3B4C1")))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")));
    }

    @Test
    @DisplayName("POST /api/patients/lookup should return 200 OK with phone and DOB body (no fullName required)")
    void lookupPatient_post_validPhoneAndDob_shouldReturn200() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class))).thenReturn(sampleLookupResponse);

        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("+1-555-0199")
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
    @DisplayName("POST /api/patients/lookup missing both patientId and (phone, DOB) should return 400 Bad Request")
    void lookupPatient_post_missingFields_shouldReturn400() throws Exception {
        when(patientLookupService.lookupPatient(any(PatientLookupRequest.class)))
                .thenThrow(new IllegalArgumentException("Either patientId or both mobile number and date of birth must be provided"));

        PatientLookupRequest request = PatientLookupRequest.builder()
                .phoneNumber("")
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
                .thenThrow(new ResourceNotFoundException("Patient not found with the provided patient ID"));

        PatientLookupRequest request = PatientLookupRequest.builder()
                .patientId("PAT-1-NONEXISTENT")
                .build();

        mockMvc.perform(post("/api/patients/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Patient not found")));
    }

    // =========================================================================
    // Get Patients List & Single Patient Tests
    // =========================================================================

    @Test
    @DisplayName("GET /api/patients should return 200 OK and list of patients")
    void getPatients_shouldReturn200() throws Exception {
        when(patientService.getPatients(null, null)).thenReturn(List.of(sampleCreatedPatientResponse));

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Patients fetched successfully")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(1)))
                .andExpect(jsonPath("$.data[0].fullName", is("John Doe")));

        verify(patientService).getPatients(null, null);
    }

    @Test
    @DisplayName("GET /api/patients with search query should pass search parameter")
    void getPatients_withSearch_shouldReturn200() throws Exception {
        when(patientService.getPatients(null, "John")).thenReturn(List.of(sampleCreatedPatientResponse));

        mockMvc.perform(get("/api/patients").param("search", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(patientService).getPatients(null, "John");
    }

    @Test
    @DisplayName("GET /api/clinics/{clinicId}/patients should pass clinicId")
    void getPatients_withClinicId_shouldReturn200() throws Exception {
        when(patientService.getPatients(1L, null)).thenReturn(List.of(sampleCreatedPatientResponse));

        mockMvc.perform(get("/api/clinics/1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(patientService).getPatients(1L, null);
    }

    @Test
    @DisplayName("GET /api/patients/{id} should return patient by ID")
    void getPatientById_shouldReturn200() throws Exception {
        when(patientService.getPatientById(1L, null)).thenReturn(sampleCreatedPatientResponse);

        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.fullName", is("John Doe")));
    }

    @Test
    @DisplayName("GET /api/patients/{id} not found should return 404")
    void getPatientById_notFound_shouldReturn404() throws Exception {
        when(patientService.getPatientById(999L, null))
                .thenThrow(new ResourceNotFoundException("Patient not found with id: 999"));

        mockMvc.perform(get("/api/patients/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Patient not found with id: 999")));
    }

    @Test
    @DisplayName("PUT /api/patients/{id} should return 200 and updated patient")
    void updatePatient_shouldReturn200() throws Exception {
        when(patientService.updatePatient(eq(1L), any(PatientRequest.class), eq(null)))
                .thenReturn(sampleCreatedPatientResponse);

        String jsonPayload = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@example.com",
                    "phoneNumber": "+1-555-0199"
                }
                """;

        mockMvc.perform(put("/api/patients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Patient updated successfully")))
                .andExpect(jsonPath("$.data.id", is(1)));
    }

    @Test
    @DisplayName("DELETE /api/patients/{id} should return 200")
    void deletePatient_shouldReturn200() throws Exception {
        doNothing().when(patientService).deletePatient(1L, null);

        mockMvc.perform(delete("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Patient deleted successfully")));

        verify(patientService).deletePatient(1L, null);
    }

    @Test
    @DisplayName("GET /api/patients/{id}/appointments/upcoming should return 200 and upcoming appointments")
    void getUpcomingAppointments_shouldReturn200() throws Exception {
        PatientUpcomingAppointmentResponse appt = PatientUpcomingAppointmentResponse.builder()
                .appointmentId(10L)
                .patientId(1L)
                .patientName("John Doe")
                .doctorName("Dr. Smith")
                .serviceName("Teeth Cleaning")
                .appointmentStart(LocalDateTime.now().plusDays(1))
                .appointmentEnd(LocalDateTime.now().plusDays(1).plusMinutes(30))
                .build();

        when(patientService.getUpcomingAppointments(1L)).thenReturn(List.of(appt));

        mockMvc.perform(get("/api/patients/1/appointments/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].appointmentId", is(10)));
    }
}
