package com.dental.manage.service.impl;

import com.dental.manage.dto.request.PatientRequest;
import com.dental.manage.dto.response.PatientResponse;
import com.dental.manage.dto.response.PatientUpcomingAppointmentResponse;
import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.Patient;
import com.dental.manage.entity.Service;
import com.dental.manage.entity.User;
import com.dental.manage.enums.UserRole;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.repository.PatientRepository;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.service.PatientService;
import com.dental.manage.util.PatientIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of PatientService.
 * Handles patient creation with secure random password generation,
 * patient queries, and retrieval of upcoming appointments.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String ALL_ALPHANUMERIC = UPPER + LOWER + DIGITS;
    private static final int PASSWORD_LENGTH = 8;

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Patient request cannot be null");
        }

        String fullName = request.getEffectiveFullName();
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Patient name is required");
        }

        String firstName = request.getEffectiveFirstName();
        String lastName = request.getEffectiveLastName();

        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        email = email.trim();

        String phoneNumber = request.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        phoneNumber = phoneNumber.trim();

        LocalDate dateOfBirth = request.getDateOfBirth();
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }
        if (dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth must be in the past");
        }

        Long clinicId = request.getClinicId() != null ? request.getClinicId() : 1L;

        // Resolve or generate unique partition-friendly string patientId
        String patientId = request.getPatientId();
        if (patientId != null && !patientId.isBlank()) {
            patientId = patientId.trim();
            if (patientRepository.findByPatientIdAndIsDeletedFalse(patientId).isPresent()) {
                throw new IllegalArgumentException("A patient with patient ID '" + patientId + "' already exists");
            }
        } else {
            patientId = PatientIdGenerator.generate(clinicId);
        }

        // Check for existing user with same username/email
        if (userRepository.existsByUsernameAndIsDeletedFalse(email)) {
            throw new IllegalArgumentException("A user with username/email '" + email + "' already exists");
        }

        // Check for existing patient with same phone number
        if (patientRepository.findByPhoneNumberAndIsDeletedFalse(phoneNumber).isPresent()) {
            throw new IllegalArgumentException("A patient with phone number '" + phoneNumber + "' already exists");
        }

        // Generate unique 8-character random alphanumeric password
        String randomPassword = generateSecureAlphanumericPassword();

        // Create and persist Patient entity
        Patient patient = Patient.builder()
                .clinicId(clinicId)
                .patientId(patientId)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phoneNumber(phoneNumber)
                .dateOfBirth(dateOfBirth)
                .isNewLead(true)
                .build();
        Patient savedPatient = patientRepository.save(patient);

        // Create and persist User entity with username = email and generated random password
        User user = User.builder()
                .clinicId(clinicId)
                .patientId(patientId)
                .username(email)
                .password(randomPassword)
                .role(UserRole.PATIENT)
                .fullName(fullName)
                .email(email)
                .phoneNumber(phoneNumber)
                .dateOfBirth(dateOfBirth)
                .isActive(true)
                .build();
        userRepository.save(user);

        log.info("Patient record created successfully with ID: {}, PatientId: {}, User account created with username: '{}'",
                savedPatient.getId(), savedPatient.getPatientId(), user.getUsername());

        return PatientResponse.builder()
                .id(savedPatient.getId())
                .patientId(savedPatient.getPatientId() != null ? savedPatient.getPatientId() : patientId)
                .clinicId(clinicId)
                .firstName(savedPatient.getFirstName())
                .lastName(savedPatient.getLastName())
                .fullName(fullName)
                .username(email)
                .password(randomPassword)
                .email(email)
                .phoneNumber(phoneNumber)
                .dateOfBirth(dateOfBirth)
                .isNewLead(savedPatient.getIsNewLead())
                .createdOn(savedPatient.getCreatedOn())
                .modifiedOn(savedPatient.getModifiedOn())
                .build();
    }

    /**
     * Generates a cryptographically secure 8-character alphanumeric password
     * ensuring at least one uppercase letter, one lowercase letter, and one digit,
     * verified to be different from any existing patient/user password.
     */
    private String generateSecureAlphanumericPassword() {
        String password;
        int attempts = 0;
        do {
            char[] chars = new char[PASSWORD_LENGTH];
            chars[0] = UPPER.charAt(secureRandom.nextInt(UPPER.length()));
            chars[1] = LOWER.charAt(secureRandom.nextInt(LOWER.length()));
            chars[2] = DIGITS.charAt(secureRandom.nextInt(DIGITS.length()));
            for (int i = 3; i < PASSWORD_LENGTH; i++) {
                chars[i] = ALL_ALPHANUMERIC.charAt(secureRandom.nextInt(ALL_ALPHANUMERIC.length()));
            }

            // Fisher-Yates shuffle
            for (int i = PASSWORD_LENGTH - 1; i > 0; i--) {
                int j = secureRandom.nextInt(i + 1);
                char temp = chars[i];
                chars[i] = chars[j];
                chars[j] = temp;
            }
            password = new String(chars);
            attempts++;
            if (attempts > 50) {
                break;
            }
        } while (userRepository.existsByPassword(password));

        return password;
    }

    @Override
    public List<PatientUpcomingAppointmentResponse> getUpcomingAppointments(Long patientId) {
        if (patientId == null) {
            throw new IllegalArgumentException("patient_id parameter is required");
        }

        log.info("Fetching upcoming appointments for patient_id: {}", patientId);

        // Verify patient exists and is not soft-deleted
        Patient patient = patientRepository.findByIdAndIsDeletedFalse(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        LocalDateTime now = LocalDateTime.now();
        List<Appointment> appointments = patientRepository.findUpcomingAppointmentsByPatientId(patientId, now);

        log.info("Found {} upcoming appointments for patient_id: {}", appointments.size(), patientId);

        return appointments.stream()
                .map(this::mapToUpcomingResponse)
                .toList();
    }

    private PatientUpcomingAppointmentResponse mapToUpcomingResponse(Appointment a) {
        Patient p = a.getPatient();
        Doctor d = a.getDoctor();
        Service s = a.getService();

        String patientFullName = (p != null) ? formatFullName(p.getFirstName(), p.getLastName()) : null;
        String doctorFullName = (d != null) ? formatFullName(d.getFirstName(), d.getLastName()) : null;

        return PatientUpcomingAppointmentResponse.builder()
                .appointmentId(a.getId())
                .patientId(p != null ? p.getId() : null)
                .patientName(patientFullName)
                .patientPhone(p != null ? p.getPhoneNumber() : null)
                .doctorId(d != null ? d.getId() : null)
                .doctorName(doctorFullName)
                .doctorSpecialization(d != null ? d.getSpecialization() : null)
                .serviceId(s != null ? s.getId() : null)
                .serviceName(s != null ? s.getName() : null)
                .serviceDurationMinutes(s != null ? s.getDurationMinutes() : null)
                .servicePrice(s != null ? s.getPrice() : null)
                .appointmentStart(a.getAppointmentStart())
                .appointmentEnd(a.getAppointmentEnd())
                .status(a.getStatus())
                .confirmationStatus(a.getConfirmationStatus())
                .build();
    }

    @Override
    public List<PatientResponse> getPatients(Long clinicId, String search) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        log.info("Fetching patients (clinicId={}, search='{}')", clinicId, cleanSearch);
        List<Patient> patients = patientRepository.findFilteredPatients(clinicId, cleanSearch);
        return patients.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PatientResponse getPatientById(Long id, Long clinicId) {
        if (id == null) {
            throw new IllegalArgumentException("Patient ID is required");
        }
        log.info("Fetching patient by id: {} (clinicId={})", id, clinicId);
        Patient patient = (clinicId != null)
                ? patientRepository.findByIdAndClinicIdAndIsDeletedFalse(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id + " for clinic: " + clinicId))
                : patientRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));

        return mapToResponse(patient);
    }

    @Override
    @Transactional
    public PatientResponse updatePatient(Long id, PatientRequest request, Long clinicId) {
        if (id == null) {
            throw new IllegalArgumentException("Patient ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Patient request cannot be null");
        }

        log.info("Updating patient id: {} (clinicId={})", id, clinicId);
        Patient patient = (clinicId != null)
                ? patientRepository.findByIdAndClinicIdAndIsDeletedFalse(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id + " for clinic: " + clinicId))
                : patientRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));

        String firstName = request.getEffectiveFirstName();
        if (firstName != null && !firstName.isBlank()) {
            patient.setFirstName(firstName.trim());
        }

        String lastName = request.getEffectiveLastName();
        if (lastName != null) {
            patient.setLastName(lastName.trim());
        }

        String email = request.getEmail();
        if (email != null && !email.isBlank()) {
            email = email.trim();
            if (!email.equalsIgnoreCase(patient.getEmail()) && userRepository.existsByUsernameAndIsDeletedFalse(email)) {
                throw new IllegalArgumentException("A user with username/email '" + email + "' already exists");
            }
            patient.setEmail(email);
        }

        String phoneNumber = request.getPhoneNumber();
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            phoneNumber = phoneNumber.trim();
            Optional<Patient> existingPhonePatient = patientRepository.findByPhoneNumberAndIsDeletedFalse(phoneNumber);
            if (existingPhonePatient.isPresent() && !existingPhonePatient.get().getId().equals(id)) {
                throw new IllegalArgumentException("A patient with phone number '" + phoneNumber + "' already exists");
            }
            patient.setPhoneNumber(phoneNumber);
        }

        if (request.getDateOfBirth() != null) {
            if (request.getDateOfBirth().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Date of birth must be in the past");
            }
            patient.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getWhatsappOptOut() != null) {
            patient.setWhatsappOptOut(request.getWhatsappOptOut());
        }

        if (request.getIsNewLead() != null) {
            patient.setIsNewLead(request.getIsNewLead());
        }

        if (clinicId != null) {
            patient.setClinicId(clinicId);
        } else if (request.getClinicId() != null) {
            patient.setClinicId(request.getClinicId());
        }

        Patient savedPatient = patientRepository.save(patient);

        // Update corresponding User record if present
        String patientId = savedPatient.getPatientId();
        if (patientId != null) {
            userRepository.findByPatientIdAndIsDeletedFalse(patientId).ifPresent(user -> {
                if (savedPatient.getEmail() != null) {
                    user.setUsername(savedPatient.getEmail());
                    user.setEmail(savedPatient.getEmail());
                }
                user.setFullName(formatFullName(savedPatient.getFirstName(), savedPatient.getLastName()));
                user.setPhoneNumber(savedPatient.getPhoneNumber());
                user.setDateOfBirth(savedPatient.getDateOfBirth());
                userRepository.save(user);
            });
        }

        return mapToResponse(savedPatient);
    }

    @Override
    @Transactional
    public void deletePatient(Long id, Long clinicId) {
        if (id == null) {
            throw new IllegalArgumentException("Patient ID is required");
        }
        log.info("Deleting patient id: {} (clinicId={})", id, clinicId);
        Patient patient = (clinicId != null)
                ? patientRepository.findByIdAndClinicIdAndIsDeletedFalse(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id + " for clinic: " + clinicId))
                : patientRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));

        patient.setIsDeleted(true);
        patientRepository.save(patient);

        if (patient.getPatientId() != null) {
            userRepository.findByPatientIdAndIsDeletedFalse(patient.getPatientId()).ifPresent(user -> {
                user.setIsDeleted(true);
                userRepository.save(user);
            });
        }
    }

    private PatientResponse mapToResponse(Patient patient) {
        if (patient == null) return null;
        String fullName = formatFullName(patient.getFirstName(), patient.getLastName());
        return PatientResponse.builder()
                .id(patient.getId())
                .patientId(patient.getPatientId())
                .clinicId(patient.getClinicId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .fullName(fullName)
                .username(patient.getEmail())
                .email(patient.getEmail())
                .phoneNumber(patient.getPhoneNumber())
                .dateOfBirth(patient.getDateOfBirth())
                .isNewLead(patient.getIsNewLead())
                .whatsappOptOut(patient.getWhatsappOptOut())
                .createdOn(patient.getCreatedOn())
                .modifiedOn(patient.getModifiedOn())
                .build();
    }

    private String formatFullName(String firstName, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return firstName != null ? firstName.trim() : "";
        }
        return (firstName != null ? firstName.trim() : "") + " " + lastName.trim();
    }
}
