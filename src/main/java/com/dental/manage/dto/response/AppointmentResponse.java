package com.dental.manage.dto.response;

import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.ConfirmationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {

    private Long id;
    private PatientResponse patient;
    private DoctorResponse doctor;
    private ServiceResponse service;
    private LocalDateTime appointmentStart;
    private LocalDateTime appointmentEnd;
    private AppointmentStatus status;
    private Boolean calendarEventAdded;
    private ConfirmationStatus confirmationStatus;
    private LocalDateTime reminderSentAt;
    private LocalDateTime createdOn;
}
