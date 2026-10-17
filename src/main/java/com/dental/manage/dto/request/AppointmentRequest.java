package com.dental.manage.dto.request;

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

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    @NotNull(message = "Appointment date/time is required")
    private LocalDateTime appointmentDateTime;

    /** Optional: Retell call record ID if booked via AI agent */
    private Long callRecordId;
}
