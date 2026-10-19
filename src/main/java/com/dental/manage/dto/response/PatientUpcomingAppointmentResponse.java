package com.dental.manage.dto.response;

import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.ConfirmationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight response DTO for a patient's upcoming appointment.
 * Omits audit fields (created_on, modified_on, created_by, etc.)
 * to provide a clean and concise payload for clients and AI agents.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientUpcomingAppointmentResponse {

    private Long appointmentId;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private Long serviceId;
    private String serviceName;
    private Integer serviceDurationMinutes;
    private BigDecimal servicePrice;
    private LocalDateTime appointmentStart;
    private LocalDateTime appointmentEnd;
    private AppointmentStatus status;
    private ConfirmationStatus confirmationStatus;
}
