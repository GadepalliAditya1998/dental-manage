package com.dental.manage.controller;

import com.dental.manage.context.ClinicRequestContext;
import com.dental.manage.dto.request.AppointmentRequest;
import com.dental.manage.dto.request.CalendarEventRequest;
import com.dental.manage.dto.response.ApiResponse;
import com.dental.manage.dto.response.AppointmentResponse;
import com.dental.manage.dto.response.TimeSlotResponse;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.BookingSource;
import com.dental.manage.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for managing appointments and booking operations.
 * Supports multi-tenant scoping via path variable (/api/clinics/{clinicId}/appointments)
 * or query parameter (?clinicId=1).
 */
@RestController
@RequestMapping({"/api/appointments", "/api/appointment", "/api/clinics/{clinicId}/appointments"})
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * Expose an API that creates an appointment booking.
     * Accepts patientId, reason for appointment, doctorId, startTime, endTime, and serviceId.
     * Supports both POST /api/appointments and POST /api/appointments/book.
     *
     * @param pathClinicId optional clinicId from path variable
     * @param queryClinicId optional clinicId from query param
     * @param request Booking details including patientId, doctorId, serviceId, startTime, endTime, reason
     * @return ApiResponse containing the created AppointmentResponse
     */
    @PostMapping({"", "/book"})
    public ResponseEntity<ApiResponse<AppointmentResponse>> createBooking(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId,
            @Valid @RequestBody AppointmentRequest request) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        if (clinicId != null && request.getClinicId() == null) {
            request.setClinicId(clinicId);
        }
        if (request.getBookingSource() == null && ClinicRequestContext.isRetellAiAgent()) {
            request.setBookingSource(BookingSource.AI_AGENT);
        }

        log.info("Request received to book appointment: patientId={}, doctorId={}, serviceId={}, startTime={}, endTime={}, reason='{}', clinicId={}",
                request.getPatientId(), request.getDoctorId(), request.getServiceId(), request.getStartTime(),
                request.getEndTime(), request.getReason(), request.getClinicId());

        AppointmentResponse response = appointmentService.bookAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Appointment booked successfully", response));
    }

    /**
     * Retrieve an appointment by its ID.
     *
     * @param id Appointment ID
     * @param pathClinicId optional clinicId from path variable
     * @param queryClinicId optional clinicId from query param
     * @return ApiResponse containing AppointmentResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointmentById(
            @PathVariable Long id,
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        log.info("Request received to fetch appointment with id: {} (clinicId={})", id, clinicId);
        AppointmentResponse response = appointmentService.getAppointmentById(id, clinicId);
        return ResponseEntity.ok(ApiResponse.success("Appointment retrieved successfully", response));
    }

    /**
     * Retrieve a list of appointments with optional filtering by clinic, doctor, patient, status, date, and bookingSource.
     *
     * @param pathClinicId optional clinicId from path variable
     * @param queryClinicId optional clinicId from query param
     * @param doctorId optional doctorId filter
     * @param patientId optional patientId filter
     * @param status optional status filter
     * @param date optional date filter (YYYY-MM-DD)
     * @param bookingSource optional booking source filter (MANUAL, AI_AGENT)
     * @param source alias for bookingSource
     * @return ApiResponse containing list of AppointmentResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointments(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "status", required = false) AppointmentStatus status,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "bookingSource", required = false) BookingSource bookingSource,
            @RequestParam(name = "source", required = false) String source) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        BookingSource effectiveSource = bookingSource != null
                ? bookingSource
                : (source != null && !source.isBlank() ? BookingSource.fromString(source) : null);

        log.info("Request received to fetch appointments (clinicId={}, doctorId={}, patientId={}, status={}, date={}, bookingSource={})",
                clinicId, doctorId, patientId, status, date, effectiveSource);

        List<AppointmentResponse> appointments = appointmentService.getAppointments(
                clinicId, doctorId, patientId, status, date, effectiveSource);
        return ResponseEntity.ok(ApiResponse.success("Appointments fetched successfully", appointments));
    }

    /**
     * Update an existing appointment.
     *
     * @param id Appointment ID
     * @param pathClinicId optional clinicId from path variable
     * @param queryClinicId optional clinicId from query param
     * @param request Update details
     * @return ApiResponse containing updated AppointmentResponse
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateAppointment(
            @PathVariable Long id,
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId,
            @Valid @RequestBody AppointmentRequest request) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        if (clinicId != null && request.getClinicId() == null) {
            request.setClinicId(clinicId);
        }
        if (request.getBookingSource() == null && ClinicRequestContext.isRetellAiAgent()) {
            request.setBookingSource(BookingSource.AI_AGENT);
        }

        log.info("Request received to update appointment with id: {}", id);
        AppointmentResponse response = appointmentService.updateAppointment(id, request, clinicId);
        return ResponseEntity.ok(ApiResponse.success("Appointment updated successfully", response));
    }

    /**
     * Update status of an appointment.
     *
     * @param id Appointment ID
     * @param pathClinicId optional clinicId from path variable
     * @param queryClinicId optional clinicId from query param
     * @param status New appointment status
     * @return ApiResponse containing updated AppointmentResponse
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable Long id,
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId,
            @RequestParam(name = "status") AppointmentStatus status) {

        Long clinicId = pathClinicId != null ? pathClinicId : queryClinicId;
        log.info("Request received to update appointment status: id={}, status={}", id, status);
        AppointmentResponse response = appointmentService.updateStatus(id, status, clinicId);
        return ResponseEntity.ok(ApiResponse.success("Appointment status updated successfully", response));
    }

    /**
     * Cancel an appointment with optional reason.
     *
     * @param id Appointment ID
     * @param pathClinicId optional clinicId from path variable
     * @param queryClinicId optional clinicId from query param
     * @param reason Optional reason for cancellation
     * @return ApiResponse containing cancelled AppointmentResponse
     */
    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            @PathVariable Long id,
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId,
            @RequestParam(name = "reason", required = false) String reason) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        log.info("Request received to cancel appointment with id: {}, reason: {}", id, reason);
        AppointmentResponse response = appointmentService.cancelAppointment(id, reason, clinicId);
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled successfully", response));
    }

    /**
     * Get available slots for a doctor on a specific date.
     *
     * @param pathClinicId optional clinicId from path variable
     * @param queryClinicId optional clinicId from query param
     * @param doctorId Doctor ID
     * @param date Date (YYYY-MM-DD)
     * @return ApiResponse containing TimeSlotResponse
     */
    @GetMapping("/slots")
    public ResponseEntity<ApiResponse<TimeSlotResponse>> getAvailableSlots(
            @PathVariable(name = "clinicId", required = false) Long pathClinicId,
            @RequestParam(name = "clinicId", required = false) Long queryClinicId,
            @RequestParam(name = "doctorId") Long doctorId,
            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Long clinicId = pathClinicId != null ? pathClinicId : (queryClinicId != null ? queryClinicId : ClinicRequestContext.getClinicId());
        log.info("Request received to get available slots for doctorId: {} on date: {}", doctorId, date);
        TimeSlotResponse response = appointmentService.getAvailableSlots(doctorId, date, clinicId);
        return ResponseEntity.ok(ApiResponse.success("Available slots retrieved successfully", response));
    }

    /**
     * Add calendar event flag to an appointment.
     *
     * @param id Appointment ID
     * @param request Optional CalendarEventRequest
     * @return ApiResponse success
     */
    @PostMapping("/{id}/calendar-event")
    public ResponseEntity<ApiResponse<Void>> addCalendarEvent(
            @PathVariable Long id,
            @RequestBody(required = false) CalendarEventRequest request) {

        if (request == null) {
            request = CalendarEventRequest.builder().appointmentId(id).build();
        } else if (request.getAppointmentId() == null) {
            request.setAppointmentId(id);
        }

        log.info("Request received to record calendar event for appointment id: {}", id);
        appointmentService.addCalendarEvent(request);
        return ResponseEntity.ok(ApiResponse.success("Calendar event recorded successfully", null));
    }
}
