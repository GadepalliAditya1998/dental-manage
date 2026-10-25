package com.dental.manage.service;

import com.dental.manage.dto.request.AppointmentRequest;
import com.dental.manage.dto.request.CalendarEventRequest;
import com.dental.manage.dto.response.AppointmentResponse;
import com.dental.manage.dto.response.TimeSlotResponse;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.BookingSource;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    /**
     * Book a new appointment.
     * Accepts patientId, reason for appointment, doctorId, startTime, endTime, and serviceId.
     *
     * @param request Appointment booking request details
     * @return Created AppointmentResponse
     */
    AppointmentResponse bookAppointment(AppointmentRequest request);

    /**
     * Retrieve appointment by ID, optionally scoped by clinic ID.
     *
     * @param id Appointment ID
     * @param clinicId Optional clinic ID
     * @return AppointmentResponse
     */
    AppointmentResponse getAppointmentById(Long id, Long clinicId);

    /**
     * Retrieve appointments matching optional filters.
     *
     * @param clinicId Optional clinic ID filter
     * @param doctorId Optional doctor ID filter
     * @param patientId Optional patient ID filter
     * @param status Optional appointment status filter
     * @param date Optional date filter
     * @param bookingSource Optional booking source filter (MANUAL or AI_AGENT)
     * @return List of AppointmentResponse
     */
    List<AppointmentResponse> getAppointments(Long clinicId, Long doctorId, Long patientId, AppointmentStatus status, LocalDate date, BookingSource bookingSource);

    default List<AppointmentResponse> getAppointments(Long clinicId, Long doctorId, Long patientId, AppointmentStatus status, LocalDate date) {
        return getAppointments(clinicId, doctorId, patientId, status, date, null);
    }

    /**
     * Update an existing appointment.
     *
     * @param id Appointment ID
     * @param request Updated appointment details
     * @param clinicId Optional clinic ID
     * @return Updated AppointmentResponse
     */
    AppointmentResponse updateAppointment(Long id, AppointmentRequest request, Long clinicId);

    /**
     * Update status of an appointment.
     *
     * @param id Appointment ID
     * @param status New appointment status
     * @param clinicId Optional clinic ID
     * @return Updated AppointmentResponse
     */
    AppointmentResponse updateStatus(Long id, AppointmentStatus status, Long clinicId);

    /**
     * Cancel an appointment with an optional cancellation reason.
     *
     * @param id Appointment ID
     * @param reason Optional reason for cancellation
     * @param clinicId Optional clinic ID
     * @return Cancelled AppointmentResponse
     */
    AppointmentResponse cancelAppointment(Long id, String reason, Long clinicId);

    /**
     * Get available time slots for a doctor on a specific date.
     *
     * @param doctorId Doctor ID
     * @param date Target date
     * @param clinicId Optional clinic ID
     * @return TimeSlotResponse with available slots
     */
    TimeSlotResponse getAvailableSlots(Long doctorId, LocalDate date, Long clinicId);

    /**
     * Flag calendar event as added for an appointment.
     *
     * @param request Calendar event request details
     */
    void addCalendarEvent(CalendarEventRequest request);
}
