package com.dental.manage.controller;

import com.dental.manage.dto.request.AppointmentRequest;
import com.dental.manage.dto.response.ApiResponse;
import com.dental.manage.dto.response.AppointmentResponse;
import com.dental.manage.dto.response.DoctorResponse;
import com.dental.manage.dto.response.PatientResponse;
import com.dental.manage.dto.response.ServiceResponse;
import com.dental.manage.dto.response.TimeSlotResponse;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.BookingSource;
import com.dental.manage.enums.ConfirmationStatus;
import com.dental.manage.exception.GlobalExceptionHandler;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.service.AppointmentService;
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
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private AppointmentController appointmentController;

    private AppointmentResponse sampleResponse;
    private LocalDateTime sampleStart;
    private LocalDateTime sampleEnd;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(appointmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleStart = LocalDateTime.of(2026, 10, 25, 10, 0, 0);
        sampleEnd = LocalDateTime.of(2026, 10, 25, 10, 30, 0);

        sampleResponse = AppointmentResponse.builder()
                .id(1L)
                .clinicId(1L)
                .patient(PatientResponse.builder()
                        .id(10L)
                        .firstName("John")
                        .lastName("Doe")
                        .phoneNumber("+1-555-0100")
                        .build())
                .doctor(DoctorResponse.builder()
                        .id(20L)
                        .firstName("Sarah")
                        .lastName("Smith")
                        .specialization("General Dentistry")
                        .build())
                .service(ServiceResponse.builder()
                        .id(30L)
                        .name("Teeth Cleaning")
                        .durationMinutes(30)
                        .build())
                .appointmentStart(sampleStart)
                .appointmentEnd(sampleEnd)
                .reason("Routine checkup and cleaning")
                .status(AppointmentStatus.SCHEDULED)
                .bookingSource(BookingSource.MANUAL)
                .confirmationStatus(ConfirmationStatus.PENDING)
                .calendarEventAdded(false)
                .build();
    }

    @Test
    @DisplayName("POST /api/appointments creates booking successfully")
    void createBooking_success() throws Exception {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(10L)
                .doctorId(20L)
                .serviceId(30L)
                .startTime(sampleStart)
                .endTime(sampleEnd)
                .reason("Routine checkup and cleaning")
                .build();

        when(appointmentService.bookAppointment(any(AppointmentRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Appointment booked successfully")))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.patient.id", is(10)))
                .andExpect(jsonPath("$.data.doctor.id", is(20)))
                .andExpect(jsonPath("$.data.service.id", is(30)))
                .andExpect(jsonPath("$.data.reason", is("Routine checkup and cleaning")))
                .andExpect(jsonPath("$.data.status", is("SCHEDULED")));

        verify(appointmentService).bookAppointment(any(AppointmentRequest.class));
    }

    @Test
    @DisplayName("POST /api/appointments/book endpoint also works for booking")
    void createBooking_viaBookSubpath() throws Exception {
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(10L)
                .doctorId(20L)
                .serviceId(30L)
                .startTime(sampleStart)
                .endTime(sampleEnd)
                .reason("Severe toothache")
                .build();

        when(appointmentService.bookAppointment(any(AppointmentRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)));
    }

    @Test
    @DisplayName("POST /api/appointments with reasonForAppointment alias in JSON")
    void createBooking_withReasonForAppointmentAlias() throws Exception {
        String jsonPayload = """
                {
                    "patientId": 10,
                    "doctorId": 20,
                    "serviceId": 30,
                    "startTime": "2026-10-25T10:00:00",
                    "endTime": "2026-10-25T10:30:00",
                    "reasonForAppointment": "Routine checkup and cleaning"
                }
                """;

        when(appointmentService.bookAppointment(any(AppointmentRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.reason", is("Routine checkup and cleaning")));
    }

    @Test
    @DisplayName("POST /api/appointments returns 400 when patientId is missing")
    void createBooking_missingPatientId_badRequest() throws Exception {
        String invalidJson = """
                {
                    "doctorId": 20,
                    "serviceId": 30,
                    "startTime": "2026-10-25T10:00:00",
                    "endTime": "2026-10-25T10:30:00",
                    "reason": "Checkup"
                }
                """;

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/appointments returns 400 when doctorId is missing")
    void createBooking_missingDoctorId_badRequest() throws Exception {
        String invalidJson = """
                {
                    "patientId": 10,
                    "serviceId": 30,
                    "startTime": "2026-10-25T10:00:00",
                    "endTime": "2026-10-25T10:30:00",
                    "reason": "Checkup"
                }
                """;

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/appointments returns 400 when serviceId is missing")
    void createBooking_missingServiceId_badRequest() throws Exception {
        String invalidJson = """
                {
                    "patientId": 10,
                    "doctorId": 20,
                    "startTime": "2026-10-25T10:00:00",
                    "endTime": "2026-10-25T10:30:00",
                    "reason": "Checkup"
                }
                """;

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/appointments returns 400 when startTime is missing")
    void createBooking_missingStartTime_badRequest() throws Exception {
        String invalidJson = """
                {
                    "patientId": 10,
                    "doctorId": 20,
                    "serviceId": 30,
                    "reason": "Checkup"
                }
                """;

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/appointments/{id} returns appointment details")
    void getAppointmentById_success() throws Exception {
        when(appointmentService.getAppointmentById(1L, null)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.reason", is("Routine checkup and cleaning")));
    }

    @Test
    @DisplayName("GET /api/appointments/{id} returns 404 when not found")
    void getAppointmentById_notFound() throws Exception {
        when(appointmentService.getAppointmentById(999L, null))
                .thenThrow(new ResourceNotFoundException("Appointment not found with id: 999"));

        mockMvc.perform(get("/api/appointments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Appointment not found with id: 999")));
    }

    @Test
    @DisplayName("GET /api/appointments returns list with filters")
    void getAppointments_withFilters() throws Exception {
        when(appointmentService.getAppointments(null, 20L, 10L, null, null, null))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/appointments")
                        .param("doctorId", "20")
                        .param("patientId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(1)))
                .andExpect(jsonPath("$.data[0].bookingSource", is("MANUAL")));
    }

    @Test
    @DisplayName("GET /api/appointments with bookingSource filter")
    void getAppointments_withBookingSourceFilter() throws Exception {
        sampleResponse.setBookingSource(BookingSource.AI_AGENT);
        when(appointmentService.getAppointments(null, null, null, null, null, BookingSource.AI_AGENT))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/appointments")
                        .param("bookingSource", "AI_AGENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].bookingSource", is("AI_AGENT")));
    }

    @Test
    @DisplayName("GET /api/appointments with source alias 'CLINIC' maps to MANUAL")
    void getAppointments_withSourceAliasClinic() throws Exception {
        when(appointmentService.getAppointments(null, null, null, null, null, BookingSource.MANUAL))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/appointments")
                        .param("source", "CLINIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/appointments with bookingSource AI_AGENT in request")
    void createBooking_withAiAgentSource() throws Exception {
        sampleResponse.setBookingSource(BookingSource.AI_AGENT);
        when(appointmentService.bookAppointment(any(AppointmentRequest.class))).thenReturn(sampleResponse);

        String jsonPayload = """
                {
                    "patientId": 10,
                    "doctorId": 20,
                    "serviceId": 30,
                    "startTime": "2026-10-25T10:00:00",
                    "endTime": "2026-10-25T10:30:00",
                    "bookingSource": "AI_AGENT"
                }
                """;

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.bookingSource", is("AI_AGENT")));
    }

    @Test
    @DisplayName("PATCH /api/appointments/{id}/status updates status")
    void updateStatus_success() throws Exception {
        sampleResponse.setStatus(AppointmentStatus.CONFIRMED);
        when(appointmentService.updateStatus(1L, AppointmentStatus.CONFIRMED, null))
                .thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/appointments/1/status")
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));
    }

    @Test
    @DisplayName("PATCH /api/appointments/{id}/cancel cancels appointment with reason")
    void cancelAppointment_success() throws Exception {
        sampleResponse.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentService.cancelAppointment(1L, "Patient cannot attend", null))
                .thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/appointments/1/cancel")
                        .param("reason", "Patient cannot attend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("GET /api/appointments/slots retrieves available slots")
    void getAvailableSlots_success() throws Exception {
        TimeSlotResponse slotResponse = TimeSlotResponse.builder()
                .doctorId(20L)
                .doctorName("Sarah Smith")
                .date("2026-10-25")
                .availableSlots(List.of(
                        TimeSlotResponse.SlotInfo.builder()
                                .startTime(LocalTime.of(9, 0))
                                .endTime(LocalTime.of(9, 30))
                                .displayTime("09:00 AM")
                                .build()
                ))
                .build();

        when(appointmentService.getAvailableSlots(20L, LocalDate.of(2026, 10, 25), null))
                .thenReturn(slotResponse);

        mockMvc.perform(get("/api/appointments/slots")
                        .param("doctorId", "20")
                        .param("date", "2026-10-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.doctorId", is(20)))
                .andExpect(jsonPath("$.data.availableSlots", hasSize(1)));
    }
}
