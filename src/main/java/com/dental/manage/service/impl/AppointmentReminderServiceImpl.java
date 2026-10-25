package com.dental.manage.service.impl;

import com.dental.manage.entity.Appointment;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.repository.AppointmentRepository;
import com.dental.manage.service.AppointmentReminderService;
import com.dental.manage.service.provider.WhatsAppProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Core implementation of AppointmentReminderService.
 * Queries scheduled appointments for the reminder window and coordinates
 * message dispatch via WhatsAppProvider.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderServiceImpl implements AppointmentReminderService {

    private final AppointmentRepository appointmentRepository;
    private final WhatsAppProvider whatsAppProvider;

    @Value("${clinic.timezone:Asia/Kolkata}")
    private String clinicTimezone;

    @Value("${clinic.reminder.days-before:1}")
    private int daysBefore;

    @Override
    @Transactional
    public int sendUpcomingAppointmentReminders() {
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(clinicTimezone);
        } catch (Exception ex) {
            log.warn("Invalid clinic.timezone '{}', falling back to system default", clinicTimezone);
            zoneId = ZoneId.systemDefault();
        }

        LocalDate targetDate = LocalDate.now(zoneId).plusDays(daysBefore);
        log.info("Executing appointment reminders for target date: {} ({} days ahead, timezone: {})",
                targetDate, daysBefore, zoneId);

        return sendRemindersForDate(targetDate);
    }

    @Override
    @Transactional
    public int sendRemindersForDate(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("Target date cannot be null");
        }

        LocalDateTime startWindow = targetDate.atStartOfDay();
        LocalDateTime endWindow = targetDate.atTime(LocalTime.MAX);

        List<Appointment> pendingAppointments = appointmentRepository.findAppointmentsNeedingReminderTomorrow(
                AppointmentStatus.SCHEDULED, startWindow, endWindow);

        log.info("Found {} scheduled appointment(s) needing reminders for {}",
                pendingAppointments.size(), targetDate);

        int sentCount = 0;
        for (Appointment appointment : pendingAppointments) {
            try {
                boolean sent = whatsAppProvider.sendAppointmentReminder(appointment);
                if (sent) {
                    appointment.setStatus(AppointmentStatus.REMINDER_SENT);
                    appointment.setReminderSentAt(LocalDateTime.now());
                    appointmentRepository.save(appointment);
                    sentCount++;
                    log.info("Reminder sent and marked REMINDER_SENT for appointment ID: {}", appointment.getId());
                } else {
                    log.warn("Reminder was not sent for appointment ID: {}", appointment.getId());
                }
            } catch (Exception e) {
                log.error("Error processing reminder for appointment ID {}: {}",
                        appointment.getId(), e.getMessage(), e);
            }
        }

        log.info("Successfully dispatched {}/{} appointment reminders for {}",
                sentCount, pendingAppointments.size(), targetDate);
        return sentCount;
    }
}
