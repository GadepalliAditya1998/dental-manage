package com.dental.manage.dto.request;

import com.dental.manage.enums.BookingSource;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequest {

    @JsonAlias({"clinic_id"})
    private Long clinicId;

    @NotNull(message = "Patient ID is required")
    @JsonAlias({"patient_id"})
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    @JsonAlias({"doctor_id"})
    private Long doctorId;

    @NotNull(message = "Service ID is required")
    @JsonAlias({"service_id"})
    private Long serviceId;

    @NotNull(message = "Start time is required")
    @JsonAlias({"appointmentStart", "start_time", "appointmentDateTime", "appointment_date_time"})
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonAlias({"appointmentEnd", "end_time"})
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    @JsonAlias({"reasonForAppointment", "reason_for_appointment", "appointmentReason", "appointment_reason"})
    private String reason;

    /** Optional: Retell call record ID if booked via AI agent */
    @JsonAlias({"call_record_id"})
    private Long callRecordId;

    /** Optional: Booking source (MANUAL from clinic side or AI_AGENT) */
    @JsonAlias({"booking_source", "bookingSource", "source", "sourceOfBooking", "source_of_booking"})
    private BookingSource bookingSource;

    // Helper compatibility getters and setters
    @JsonProperty("reasonForAppointment")
    public String getReasonForAppointment() {
        return reason;
    }

    public void setReasonForAppointment(String reasonForAppointment) {
        if (this.reason == null) {
            this.reason = reasonForAppointment;
        }
    }

    @JsonProperty("appointmentDateTime")
    public LocalDateTime getAppointmentDateTime() {
        return startTime;
    }

    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) {
        if (this.startTime == null) {
            this.startTime = appointmentDateTime;
        }
    }

    @JsonProperty("appointmentStart")
    public LocalDateTime getAppointmentStart() {
        return startTime;
    }

    public void setAppointmentStart(LocalDateTime appointmentStart) {
        if (this.startTime == null) {
            this.startTime = appointmentStart;
        }
    }

    @JsonProperty("appointmentEnd")
    public LocalDateTime getAppointmentEnd() {
        return endTime;
    }

    public void setAppointmentEnd(LocalDateTime appointmentEnd) {
        if (this.endTime == null) {
            this.endTime = appointmentEnd;
        }
    }
}
