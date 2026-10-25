package com.dental.manage.controller;

import com.dental.manage.service.AppointmentReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for manual triggers and management of appointment reminders.
 * Allows on-demand triggering for testing or administrative operational tasks.
 */
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderController {

    private final AppointmentReminderService reminderService;

    /**
     * Manually triggers the appointment reminder job for tomorrow (or an optionally specified date).
     *
     * @param date Optional target date (format: yyyy-MM-dd). If omitted, defaults to tomorrow.
     * @return Execution summary with sent count
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerReminders(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Manual trigger requested for appointment reminders. Target date: {}", date);

        int sentCount;
        LocalDate effectiveDate;

        if (date != null) {
            sentCount = reminderService.sendRemindersForDate(date);
            effectiveDate = date;
        } else {
            sentCount = reminderService.sendUpcomingAppointmentReminders();
            effectiveDate = LocalDate.now().plusDays(1);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Appointment reminders processed successfully");
        response.put("remindersSent", sentCount);
        response.put("targetDate", effectiveDate.toString());

        return ResponseEntity.ok(response);
    }
}
