package com.dental.manage.service.impl;

import com.dental.manage.context.ClinicRequestContext;
import com.dental.manage.dto.request.DoctorAvailabilityRequest;
import com.dental.manage.dto.request.DoctorRequest;
import com.dental.manage.dto.response.DoctorAvailabilityResponse;
import com.dental.manage.dto.response.DoctorResponse;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.DoctorAvailability;
import com.dental.manage.entity.User;
import com.dental.manage.enums.UserRole;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.mapper.DoctorMapper;
import com.dental.manage.repository.DoctorAvailabilityRepository;
import com.dental.manage.repository.DoctorRepository;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.service.DoctorManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of DoctorManagementService.
 * Uses the users table with role = DOCTOR as the primary source of truth for doctors,
 * while keeping Doctor entity synchronized for appointment foreign key relations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DoctorManagementServiceImpl implements DoctorManagementService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final DoctorMapper doctorMapper;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Override
    public List<DoctorResponse> getDoctors(Long clinicId, Long serviceId, Boolean activeOnly) {
        Long effectiveClinicId = clinicId != null ? clinicId : ClinicRequestContext.getClinicId();
        log.info("Fetching doctors from users table: clinicId={}, serviceId={}, activeOnly={}",
                effectiveClinicId, serviceId, activeOnly);

        List<User> doctorUsers = userRepository.findDoctors(effectiveClinicId, Boolean.TRUE.equals(activeOnly));

        return doctorUsers.stream()
                .map(this::mapUserToDoctorResponse)
                .toList();
    }

    @Override
    public DoctorResponse getDoctorById(Long id, Long clinicId) {
        User doctorUser = findDoctorUser(id, clinicId);
        return mapUserToDoctorResponse(doctorUser);
    }

    @Override
    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request, Long clinicId) {
        Long effectiveClinicId = request.getClinicId() != null
                ? request.getClinicId()
                : (clinicId != null ? clinicId : ClinicRequestContext.getClinicId());

        if (effectiveClinicId == null) {
            effectiveClinicId = 1L;
        }

        String fullName = (request.getFirstName() + " " + (request.getLastName() != null ? request.getLastName() : "")).trim();
        log.info("Creating doctor user in users table: fullName='{}', clinicId={}", fullName, effectiveClinicId);

        String username = generateUniqueDoctorUsername(request.getEmail(), request.getFirstName(), request.getLastName());

        User doctorUser = User.builder()
                .clinicId(effectiveClinicId)
                .username(username)
                .password("doctor123") // default password for portal login
                .role(UserRole.DOCTOR)
                .fullName(fullName)
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        User savedUser = userRepository.save(doctorUser);
        log.info("Doctor user created in users table with id={}", savedUser.getId());

        // Keep Doctor entity in sync for foreign key integrity in appointments table
        syncDoctorEntity(savedUser, request.getSpecialization());

        return mapUserToDoctorResponse(savedUser);
    }

    @Override
    @Transactional
    public DoctorResponse updateDoctor(Long id, DoctorRequest request, Long clinicId) {
        User doctorUser = findDoctorUser(id, clinicId);
        log.info("Updating doctor user id={} in users table", id);

        String fullName = (request.getFirstName() + " " + (request.getLastName() != null ? request.getLastName() : "")).trim();
        doctorUser.setFullName(fullName);

        if (request.getEmail() != null) {
            doctorUser.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            doctorUser.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getIsActive() != null) {
            doctorUser.setIsActive(request.getIsActive());
        }
        if (request.getClinicId() != null) {
            doctorUser.setClinicId(request.getClinicId());
        }

        User updatedUser = userRepository.save(doctorUser);

        // Keep Doctor entity in sync
        syncDoctorEntity(updatedUser, request.getSpecialization());

        return mapUserToDoctorResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteDoctor(Long id, Long clinicId) {
        User doctorUser = findDoctorUser(id, clinicId);
        log.info("Soft-deleting doctor user id={} in users table", id);
        doctorUser.setIsDeleted(true);
        userRepository.save(doctorUser);

        // Soft-delete matching doctor entity if present
        doctorRepository.findByIdAndIsDeletedFalse(id).ifPresent(d -> {
            d.setIsDeleted(true);
            doctorRepository.save(d);
        });
    }

    @Override
    public List<DoctorAvailabilityResponse> getDoctorAvailability(Long doctorId, Long clinicId) {
        // Ensure doctor user exists in users table
        findDoctorUser(doctorId, clinicId);

        List<DoctorAvailability> availabilities = (clinicId != null)
                ? doctorAvailabilityRepository.findByClinicIdAndDoctorIdAndIsActiveTrueAndIsDeletedFalse(clinicId, doctorId)
                : doctorAvailabilityRepository.findByDoctorIdAndIsDeletedFalse(doctorId);

        return doctorMapper.toAvailabilityResponseList(availabilities);
    }

    @Override
    @Transactional
    public List<DoctorAvailabilityResponse> updateDoctorAvailability(
            Long doctorId, List<DoctorAvailabilityRequest> availabilities, Long clinicId) {

        User doctorUser = findDoctorUser(doctorId, clinicId);
        Doctor doctor = syncDoctorEntity(doctorUser, null);

        log.info("Updating availability for doctor id={}, slots count={}", doctorId, availabilities != null ? availabilities.size() : 0);

        List<DoctorAvailability> existing = doctorAvailabilityRepository.findByDoctorIdAndIsDeletedFalse(doctorId);
        for (DoctorAvailability da : existing) {
            da.setIsDeleted(true);
        }
        doctorAvailabilityRepository.saveAll(existing);

        List<DoctorAvailability> newEntities = new ArrayList<>();
        if (availabilities != null) {
            for (DoctorAvailabilityRequest req : availabilities) {
                DoctorAvailability entity = doctorMapper.toAvailabilityEntity(req);
                entity.setDoctor(doctor);
                entity.setClinicId(doctor.getClinicId());
                if (entity.getIsActive() == null) {
                    entity.setIsActive(true);
                }
                newEntities.add(entity);
            }
        }

        List<DoctorAvailability> saved = doctorAvailabilityRepository.saveAll(newEntities);
        return doctorMapper.toAvailabilityResponseList(saved);
    }

    private User findDoctorUser(Long id, Long clinicId) {
        Long effectiveClinicId = clinicId != null ? clinicId : ClinicRequestContext.getClinicId();
        if (effectiveClinicId != null) {
            return userRepository.findByIdAndClinicIdAndRoleAndIsDeletedFalse(id, effectiveClinicId, UserRole.DOCTOR)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
        }
        return userRepository.findByIdAndRoleAndIsDeletedFalse(id, UserRole.DOCTOR)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
    }

    private DoctorResponse mapUserToDoctorResponse(User user) {
        if (user == null) return null;

        String firstName = "";
        String lastName = "";
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            String trimmed = user.getFullName().trim();
            int lastSpaceIndex = trimmed.lastIndexOf(' ');
            if (lastSpaceIndex > 0) {
                firstName = trimmed.substring(0, lastSpaceIndex).trim();
                lastName = trimmed.substring(lastSpaceIndex + 1).trim();
            } else {
                firstName = trimmed;
                lastName = "";
            }
        } else if (user.getUsername() != null) {
            firstName = user.getUsername();
        }

        // Check if Doctor entity has a specialization configured
        String specialization = doctorRepository.findByIdAndIsDeletedFalse(user.getId())
                .map(Doctor::getSpecialization)
                .filter(s -> !s.isBlank())
                .orElse("General Dentistry");

        return DoctorResponse.builder()
                .id(user.getId())
                .clinicId(user.getClinicId())
                .firstName(firstName)
                .lastName(lastName)
                .specialization(specialization)
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                .createdOn(user.getCreatedOn())
                .build();
    }

    @Override
    @Transactional
    public Doctor syncDoctorEntity(User user, String specialization) {
        if (user == null || user.getId() == null) {
            return null;
        }

        Doctor doctor = doctorRepository.findByIdAndIsDeletedFalse(user.getId())
                .or(() -> user.getEmail() != null ? doctorRepository.findByEmailIgnoreCaseAndIsDeletedFalse(user.getEmail()) : Optional.empty())
                .orElse(null);

        String firstName = "";
        String lastName = "";
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            String trimmed = user.getFullName().trim();
            int lastSpaceIndex = trimmed.lastIndexOf(' ');
            if (lastSpaceIndex > 0) {
                firstName = trimmed.substring(0, lastSpaceIndex).trim();
                lastName = trimmed.substring(lastSpaceIndex + 1).trim();
            } else {
                firstName = trimmed;
                lastName = "Doctor";
            }
        } else if (user.getUsername() != null) {
            firstName = user.getUsername();
            lastName = "Doctor";
        }

        String spec = specialization != null ? specialization : "General Dentistry";
        Long clinicId = user.getClinicId() != null ? user.getClinicId() : 1L;
        boolean isActive = user.getIsActive() != null ? user.getIsActive() : true;

        if (doctor == null) {
            doctor = Doctor.builder()
                    .clinicId(clinicId)
                    .firstName(firstName)
                    .lastName(lastName)
                    .specialization(spec)
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .isActive(isActive)
                    .build();
            doctor.setId(user.getId());

            try {
                return doctorRepository.save(doctor);
            } catch (Exception ex) {
                log.warn("JPA save failed for new doctor id={}, falling back to native insert: {}", user.getId(), ex.getMessage());
                if (entityManager != null) {
                    entityManager.createNativeQuery("""
                        INSERT INTO doctors (id, clinic_id, first_name, last_name, specialization, email, phone_number, is_active, is_deleted, created_by, created_on, modified_by, modified_on)
                        VALUES (:id, :clinicId, :firstName, :lastName, :specialization, :email, :phoneNumber, :isActive, false, 'SYSTEM', now(), 'SYSTEM', now())
                        ON CONFLICT (id) DO UPDATE SET
                            clinic_id = EXCLUDED.clinic_id,
                            first_name = EXCLUDED.first_name,
                            last_name = EXCLUDED.last_name,
                            specialization = EXCLUDED.specialization,
                            email = EXCLUDED.email,
                            phone_number = EXCLUDED.phone_number,
                            is_active = EXCLUDED.is_active,
                            is_deleted = false,
                            modified_on = now()
                    """)
                    .setParameter("id", user.getId())
                    .setParameter("clinicId", clinicId)
                    .setParameter("firstName", firstName)
                    .setParameter("lastName", lastName)
                    .setParameter("specialization", spec)
                    .setParameter("email", user.getEmail())
                    .setParameter("phoneNumber", user.getPhoneNumber())
                    .setParameter("isActive", isActive)
                    .executeUpdate();

                    return doctorRepository.findByIdAndIsDeletedFalse(user.getId()).orElse(doctor);
                }
                throw ex;
            }
        } else {
            doctor.setFirstName(firstName);
            doctor.setLastName(lastName);
            doctor.setEmail(user.getEmail());
            doctor.setPhoneNumber(user.getPhoneNumber());
            doctor.setIsActive(isActive);
            if (specialization != null) {
                doctor.setSpecialization(specialization);
            }
            return doctorRepository.save(doctor);
        }
    }

    private String generateUniqueDoctorUsername(String email, String firstName, String lastName) {
        String base;
        if (email != null && !email.isBlank()) {
            base = email.trim().toLowerCase();
        } else {
            String first = firstName != null ? firstName.trim().toLowerCase() : "dr";
            String last = lastName != null ? lastName.trim().toLowerCase() : "doctor";
            base = "dr." + first + "." + last;
        }

        String candidate = base;
        int counter = 1;
        while (userRepository.existsByUsernameAndIsDeletedFalse(candidate)) {
            candidate = base + counter++;
        }
        return candidate;
    }
}
