package com.dental.manage.entity;

import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.ConfirmationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Core appointment/booking entity linking patient, doctor, and service.
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends BaseEntity {

    @Column(name = "clinic_id")
    private Long clinicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_record_id")
    private CallRecord callRecord;

    @Column(name = "appointment_start", nullable = false)
    private LocalDateTime appointmentStart;

    @Column(name = "appointment_end", nullable = false)
    private LocalDateTime appointmentEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(name = "calendar_event_added")
    @Builder.Default
    private Boolean calendarEventAdded = false;

    @Column(name = "calendar_event_id")
    private String calendarEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmation_status", length = 20)
    @Builder.Default
    private ConfirmationStatus confirmationStatus = ConfirmationStatus.PENDING;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;
}
