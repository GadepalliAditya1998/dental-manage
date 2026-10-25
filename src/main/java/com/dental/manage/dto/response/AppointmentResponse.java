package com.dental.manage.dto.response;

import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.BookingSource;
import com.dental.manage.enums.ConfirmationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private Long clinicId;
    private PatientResponse patient;
    private DoctorResponse doctor;
    private ServiceResponse service;
    private CallRecordResponse callRecord;
    private LocalDateTime appointmentStart;
    private LocalDateTime appointmentEnd;
    private String reason;
    private AppointmentStatus status;
    private Boolean calendarEventAdded;
    private String calendarEventId;
    private ConfirmationStatus confirmationStatus;
    private BookingSource bookingSource;
    private LocalDateTime reminderSentAt;
    private LocalDateTime createdOn;

    @JsonProperty("startTime")
    public LocalDateTime getStartTime() {
        return appointmentStart;
    }

    @JsonProperty("endTime")
    public LocalDateTime getEndTime() {
        return appointmentEnd;
    }

    @JsonProperty("reasonForAppointment")
    public String getReasonForAppointment() {
        return reason;
    }
}
