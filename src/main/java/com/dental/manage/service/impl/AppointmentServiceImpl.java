package com.dental.manage.service.impl;

import com.dental.manage.context.ClinicRequestContext;
import com.dental.manage.dto.request.AppointmentRequest;
import com.dental.manage.dto.request.CalendarEventRequest;
import com.dental.manage.dto.response.AppointmentResponse;
import com.dental.manage.dto.response.TimeSlotResponse;
import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.CallRecord;
import com.dental.manage.entity.ClinicDetails;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.DoctorAvailability;
import com.dental.manage.entity.Patient;
import com.dental.manage.entity.Service;
import com.dental.manage.entity.User;
import com.dental.manage.enums.AppointmentStatus;
import com.dental.manage.enums.BookingSource;
import com.dental.manage.enums.ConfirmationStatus;
import com.dental.manage.enums.UserRole;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.mapper.AppointmentMapper;
import com.dental.manage.repository.AppointmentRepository;
import com.dental.manage.repository.CallRecordRepository;
import com.dental.manage.repository.ClinicDetailsRepository;
import com.dental.manage.repository.DoctorAvailabilityRepository;
import com.dental.manage.repository.DoctorRepository;
import com.dental.manage.repository.PatientRepository;
import com.dental.manage.repository.ServiceRepository;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.service.AppointmentService;
import com.dental.manage.service.CalendarService;
import com.dental.manage.service.DoctorManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ServiceRepository serviceRepository;
    private final CallRecordRepository callRecordRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final ClinicDetailsRepository clinicDetailsRepository;
    private final AppointmentMapper appointmentMapper;
    private final CalendarService calendarService;
    private final UserRepository userRepository;
    private final DoctorManagementService doctorManagementService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    @Override
    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Appointment request cannot be null");
        }
        if (request.getPatientId() == null) {
            throw new IllegalArgumentException("Patient ID is required");
        }
        if (request.getDoctorId() == null) {
            throw new IllegalArgumentException("Doctor ID is required");
        }
        if (request.getServiceId() == null) {
            throw new IllegalArgumentException("Service ID is required");
        }
        if (request.getStartTime() == null) {
            throw new IllegalArgumentException("Start time is required");
        }

        log.info("Booking appointment for patientId={}, doctorId={}, serviceId={}, startTime={}, reason='{}'",
                request.getPatientId(), request.getDoctorId(), request.getServiceId(), request.getStartTime(), request.getReason());

        // 1. Verify Patient
        Patient patient = patientRepository.findByIdAndIsDeletedFalse(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + request.getPatientId()));

        // 2. Verify Doctor
        Doctor doctor = resolveDoctor(request.getDoctorId(), request.getClinicId());
        if (doctor == null) {
            throw new ResourceNotFoundException("Doctor not found with id: " + request.getDoctorId());
        }
        if (Boolean.FALSE.equals(doctor.getIsActive())) {
            throw new IllegalArgumentException("Doctor with id " + doctor.getId() + " is currently inactive");
        }

        // 3. Verify Service
        Service service = serviceRepository.findByIdAndIsDeletedFalse(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + request.getServiceId()));
        if (Boolean.FALSE.equals(service.getIsActive())) {
            throw new IllegalArgumentException("Service with id " + service.getId() + " is currently inactive");
        }

        // 4. Determine and validate appointment time window
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        if (endTime == null) {
            int duration = (service.getDurationMinutes() != null && service.getDurationMinutes() > 0)
                    ? service.getDurationMinutes()
                    : 30;
            endTime = startTime.plusMinutes(duration);
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Appointment end time (" + endTime + ") must be after start time (" + startTime + ")");
        }

        // 5. Conflict check: verify doctor is not double-booked
        List<Appointment> conflicts = appointmentRepository.findOverlappingDoctorAppointments(doctor.getId(), startTime, endTime);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Doctor '%s %s' already has an appointment scheduled between %s and %s",
                    doctor.getFirstName(),
                    doctor.getLastName() != null ? doctor.getLastName() : "",
                    startTime,
                    endTime));
        }

        // 6. Resolve multi-tenant clinicId
        Long clinicId = request.getClinicId();
        if (clinicId == null && ClinicRequestContext.getClinicId() != null) {
            clinicId = ClinicRequestContext.getClinicId();
        }
        if (clinicId == null) {
            clinicId = doctor.getClinicId() != null ? doctor.getClinicId()
                    : (patient.getClinicId() != null ? patient.getClinicId()
                    : (service.getClinicId() != null ? service.getClinicId() : 1L));
        }

        // 7. Optional AI call record association & booking source resolution
        CallRecord callRecord = null;
        if (request.getCallRecordId() != null) {
            callRecord = callRecordRepository.findByIdAndIsDeletedFalse(request.getCallRecordId()).orElse(null);
        }

        BookingSource bookingSource = request.getBookingSource();
        if (bookingSource == null) {
            if (ClinicRequestContext.isRetellAiAgent()) {
                bookingSource = BookingSource.AI_AGENT;
            } else if (callRecord != null || request.getCallRecordId() != null) {
                bookingSource = BookingSource.AI_AGENT;
            } else {
                bookingSource = BookingSource.MANUAL;
            }
        }

        // 8. Build and persist entity
        Appointment appointment = Appointment.builder()
                .clinicId(clinicId)
                .patient(patient)
                .doctor(doctor)
                .service(service)
                .callRecord(callRecord)
                .appointmentStart(startTime)
                .appointmentEnd(endTime)
                .reason(request.getReason())
                .status(AppointmentStatus.SCHEDULED)
                .confirmationStatus(ConfirmationStatus.PENDING)
                .calendarEventAdded(false)
                .bookingSource(bookingSource)
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment successfully booked with id: {}", saved.getId());

        // Google Calendar synchronization (reflect on doctor's and patient's Google Calendar)
        try {
            calendarService.createAppointmentEvent(saved).ifPresent(eventId -> {
                saved.setCalendarEventId(eventId);
                saved.setCalendarEventAdded(true);
                appointmentRepository.save(saved);
                log.info("Google Calendar event created: id={} for appointment id={}", eventId, saved.getId());
            });
        } catch (Exception e) {
            log.error("Google Calendar event creation failed for appointment id {}: {}", saved.getId(), e.getMessage());
        }

        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id, Long clinicId) {
        Appointment appointment = (clinicId != null)
                ? appointmentRepository.findByIdAndClinicIdAndIsDeletedFalse(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id + " for clinic: " + clinicId))
                : appointmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        return appointmentMapper.toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments(Long clinicId, Long doctorId, Long patientId, AppointmentStatus status, LocalDate date, BookingSource bookingSource) {
        LocalDateTime start = (date != null) ? date.atStartOfDay() : null;
        LocalDateTime end = (date != null) ? date.atTime(LocalTime.MAX) : null;

        List<Appointment> appointments = appointmentRepository.findFilteredAppointments(
                clinicId, doctorId, patientId, status, bookingSource, start, end);
        return appointmentMapper.toResponseList(appointments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments(Long clinicId, Long doctorId, Long patientId, AppointmentStatus status, LocalDate date) {
        return getAppointments(clinicId, doctorId, patientId, status, date, null);
    }

    @Override
    public AppointmentResponse updateAppointment(Long id, AppointmentRequest request, Long clinicId) {
        Appointment appointment = (clinicId != null)
                ? appointmentRepository.findByIdAndClinicIdAndIsDeletedFalse(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id + " for clinic: " + clinicId))
                : appointmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        if (request.getPatientId() != null && !request.getPatientId().equals(appointment.getPatient().getId())) {
            Patient patient = patientRepository.findByIdAndIsDeletedFalse(request.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + request.getPatientId()));
            appointment.setPatient(patient);
        }

        if (request.getDoctorId() != null && !request.getDoctorId().equals(appointment.getDoctor().getId())) {
            Doctor doctor = resolveDoctor(request.getDoctorId(), clinicId);
            if (doctor == null) {
                throw new ResourceNotFoundException("Doctor not found with id: " + request.getDoctorId());
            }
            if (Boolean.FALSE.equals(doctor.getIsActive())) {
                throw new IllegalArgumentException("Doctor with id " + doctor.getId() + " is currently inactive");
            }
            appointment.setDoctor(doctor);
        }

        if (request.getServiceId() != null && !request.getServiceId().equals(appointment.getService().getId())) {
            Service service = serviceRepository.findByIdAndIsDeletedFalse(request.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + request.getServiceId()));
            if (Boolean.FALSE.equals(service.getIsActive())) {
                throw new IllegalArgumentException("Service with id " + service.getId() + " is currently inactive");
            }
            appointment.setService(service);
        }

        LocalDateTime startTime = request.getStartTime() != null ? request.getStartTime() : appointment.getAppointmentStart();
        LocalDateTime endTime = request.getEndTime() != null ? request.getEndTime() : appointment.getAppointmentEnd();

        if (request.getStartTime() != null && request.getEndTime() == null) {
            int duration = (appointment.getService().getDurationMinutes() != null && appointment.getService().getDurationMinutes() > 0)
                    ? appointment.getService().getDurationMinutes() : 30;
            endTime = startTime.plusMinutes(duration);
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Appointment end time (" + endTime + ") must be after start time (" + startTime + ")");
        }

        // Verify conflict excluding current appointment
        List<Appointment> conflicts = appointmentRepository.findOverlappingDoctorAppointmentsExcluding(
                appointment.getDoctor().getId(), startTime, endTime, appointment.getId());
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Doctor has a conflicting appointment during this time window: "
                    + startTime + " to " + endTime);
        }

        appointment.setAppointmentStart(startTime);
        appointment.setAppointmentEnd(endTime);
        if (request.getReason() != null) {
            appointment.setReason(request.getReason());
        }
        if (request.getBookingSource() != null) {
            appointment.setBookingSource(request.getBookingSource());
        }

        Appointment updated = appointmentRepository.save(appointment);
        log.info("Appointment updated successfully with id: {}", updated.getId());

        // Reflect updates on Google Calendar (updates doctor's and patient's calendars)
        try {
            calendarService.updateAppointmentEvent(updated);
            if (updated.getCalendarEventId() != null && !Boolean.TRUE.equals(updated.getCalendarEventAdded())) {
                updated.setCalendarEventAdded(true);
                updated = appointmentRepository.save(updated);
            }
        } catch (Exception e) {
            log.error("Google Calendar update sync failed for appointment id {}: {}", updated.getId(), e.getMessage());
        }

        return appointmentMapper.toResponse(updated);
    }

    @Override
    public AppointmentResponse updateStatus(Long id, AppointmentStatus status, Long clinicId) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        Appointment appointment = (clinicId != null)
                ? appointmentRepository.findByIdAndClinicIdAndIsDeletedFalse(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id + " for clinic: " + clinicId))
                : appointmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        appointment.setStatus(status);
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Status updated to {} for appointment id: {}", status, id);

        // Reflect status updates on Google Calendar
        try {
            if (status == AppointmentStatus.CANCELLED) {
                calendarService.cancelAppointmentEvent(saved, "Status changed to CANCELLED");
            } else {
                calendarService.updateAppointmentEvent(saved);
            }
        } catch (Exception e) {
            log.error("Google Calendar status update sync failed for appointment id {}: {}", id, e.getMessage());
        }

        return appointmentMapper.toResponse(saved);
    }

    @Override
    public AppointmentResponse cancelAppointment(Long id, String reason, Long clinicId) {
        Appointment appointment = (clinicId != null)
                ? appointmentRepository.findByIdAndClinicIdAndIsDeletedFalse(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id + " for clinic: " + clinicId))
                : appointmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            String updatedReason = (appointment.getReason() != null && !appointment.getReason().isBlank())
                    ? appointment.getReason() + " [Cancellation reason: " + reason.trim() + "]"
                    : "[Cancellation reason: " + reason.trim() + "]";
            appointment.setReason(updatedReason);
        }

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment cancelled with id: {}", id);

        // Reflect cancellation on Google Calendar (notifies doctor and patient and cancels event)
        try {
            calendarService.cancelAppointmentEvent(saved, reason);
        } catch (Exception e) {
            log.error("Google Calendar cancellation sync failed for appointment id {}: {}", id, e.getMessage());
        }

        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TimeSlotResponse getAvailableSlots(Long doctorId, LocalDate date, Long clinicId) {
        if (doctorId == null) {
            throw new IllegalArgumentException("Doctor ID is required");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }

        Doctor doctor = resolveDoctor(doctorId, clinicId);
        if (doctor == null) {
            throw new ResourceNotFoundException("Doctor not found with id: " + doctorId);
        }

        String dayOfWeek = date.getDayOfWeek().name();
        List<DoctorAvailability> availabilities = doctorAvailabilityRepository
                .findByDoctorIdAndDayOfWeekAndIsActiveTrueAndIsDeletedFalse(doctorId, dayOfWeek);

        // Fetch booked appointments on that date
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        List<Appointment> bookedAppointments = appointmentRepository.findByDoctorIdAndDateRange(doctorId, dayStart, dayEnd);

        List<TimeSlotResponse.SlotInfo> availableSlots = new ArrayList<>();
        int slotDurationMinutes = 30;

        if (availabilities.isEmpty()) {
            // Fallback to clinic opening/closing hours or 09:00 - 17:00
            LocalTime defaultStart = LocalTime.of(9, 0);
            LocalTime defaultEnd = LocalTime.of(17, 0);
            if (clinicId != null) {
                clinicDetailsRepository.findByIdAndIsDeletedFalse(clinicId).ifPresent(clinic -> {
                    // if clinic has opening/closing times
                    if (clinic.getOpeningTime() != null) defaultStart.withHour(clinic.getOpeningTime().getHour());
                });
            }
            generateSlots(date, defaultStart, defaultEnd, slotDurationMinutes, bookedAppointments, availableSlots);
        } else {
            for (DoctorAvailability availability : availabilities) {
                generateSlots(date, availability.getStartTime(), availability.getEndTime(),
                        slotDurationMinutes, bookedAppointments, availableSlots);
            }
        }

        String doctorFullName = doctor.getFirstName() + (doctor.getLastName() != null ? " " + doctor.getLastName() : "");
        return TimeSlotResponse.builder()
                .doctorId(doctorId)
                .doctorName(doctorFullName)
                .date(date.toString())
                .availableSlots(availableSlots)
                .build();
    }

    private void generateSlots(LocalDate date, LocalTime startTime, LocalTime endTime, int durationMinutes,
                               List<Appointment> bookedAppointments, List<TimeSlotResponse.SlotInfo> availableSlots) {
        LocalTime current = startTime;
        while (current.plusMinutes(durationMinutes).isBefore(endTime) || current.plusMinutes(durationMinutes).equals(endTime)) {
            LocalTime slotEnd = current.plusMinutes(durationMinutes);
            LocalDateTime slotStartDt = date.atTime(current);
            LocalDateTime slotEndDt = date.atTime(slotEnd);

            boolean isBooked = bookedAppointments.stream().anyMatch(appt ->
                    appt.getAppointmentStart().isBefore(slotEndDt) && appt.getAppointmentEnd().isAfter(slotStartDt));

            if (!isBooked) {
                availableSlots.add(TimeSlotResponse.SlotInfo.builder()
                        .startTime(current)
                        .endTime(slotEnd)
                        .displayTime(current.format(TIME_FORMATTER))
                        .build());
            }
            current = slotEnd;
        }
    }

    @Override
    public void addCalendarEvent(CalendarEventRequest request) {
        if (request == null || request.getAppointmentId() == null) {
            throw new IllegalArgumentException("Appointment ID is required");
        }
        Appointment appointment = appointmentRepository.findByIdAndIsDeletedFalse(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + request.getAppointmentId()));

        // Update patient email if provided in request and missing/changed on patient
        if (request.getPatientEmail() != null && !request.getPatientEmail().isBlank()) {
            Patient patient = appointment.getPatient();
            if (patient != null && (patient.getEmail() == null || !patient.getEmail().equalsIgnoreCase(request.getPatientEmail().trim()))) {
                patient.setEmail(request.getPatientEmail().trim());
                patientRepository.save(patient);
            }
        }

        // Sync with Google Calendar
        try {
            calendarService.syncAppointmentEvent(appointment).ifPresent(eventId -> {
                appointment.setCalendarEventId(eventId);
                appointment.setCalendarEventAdded(true);
            });
            appointment.setCalendarEventAdded(true);
            appointmentRepository.save(appointment);
            log.info("Calendar event synced for appointment id: {}", appointment.getId());
        } catch (Exception e) {
            log.error("Failed to sync calendar event for appointment id {}: {}", appointment.getId(), e.getMessage(), e);
            appointment.setCalendarEventAdded(true);
            appointmentRepository.save(appointment);
        }
    }

    private Doctor resolveDoctor(Long doctorId, Long clinicId) {
        if (doctorId == null) {
            return null;
        }

        Optional<Doctor> existing = doctorRepository.findByIdAndIsDeletedFalse(doctorId);
        if (existing.isPresent()) {
            return existing.get();
        }

        if (userRepository != null && doctorManagementService != null) {
            Optional<User> doctorUser = (clinicId != null)
                    ? userRepository.findByIdAndClinicIdAndRoleAndIsDeletedFalse(doctorId, clinicId, UserRole.DOCTOR)
                            .or(() -> userRepository.findByIdAndRoleAndIsDeletedFalse(doctorId, UserRole.DOCTOR))
                    : userRepository.findByIdAndRoleAndIsDeletedFalse(doctorId, UserRole.DOCTOR);

            if (doctorUser.isPresent()) {
                log.info("Auto-syncing missing Doctor record for DOCTOR user id={}", doctorId);
                return doctorManagementService.syncDoctorEntity(doctorUser.get(), "General Dentistry");
            }
        }

        return null;
    }
}
