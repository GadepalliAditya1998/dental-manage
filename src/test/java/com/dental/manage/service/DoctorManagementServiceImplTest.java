package com.dental.manage.service;

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
import com.dental.manage.service.impl.DoctorManagementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorManagementServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorAvailabilityRepository doctorAvailabilityRepository;

    @Mock
    private DoctorMapper doctorMapper;

    @InjectMocks
    private DoctorManagementServiceImpl doctorManagementService;

    private User doctorUser;
    private Doctor doctorEntity;

    @BeforeEach
    void setUp() {
        doctorUser = User.builder()
                .clinicId(1L)
                .username("doctor")
                .fullName("Dr. Jane Smith")
                .role(UserRole.DOCTOR)
                .email("jane.smith@dental.com")
                .phoneNumber("+1234567890")
                .isActive(true)
                .build();
        doctorUser.setId(10L);

        doctorEntity = Doctor.builder()
                .clinicId(1L)
                .firstName("Dr. Jane")
                .lastName("Smith")
                .specialization("General Dentistry")
                .email("jane.smith@dental.com")
                .phoneNumber("+1234567890")
                .isActive(true)
                .build();
        doctorEntity.setId(10L);
    }

    @Test
    @DisplayName("getDoctors should search in users table where role = DOCTOR")
    void getDoctors_shouldReturnListFromUsersTable() {
        when(userRepository.findDoctors(1L, false)).thenReturn(List.of(doctorUser));

        List<DoctorResponse> result = doctorManagementService.getDoctors(1L, null, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(0).getLastName()).isEqualTo("Smith");
        verify(userRepository).findDoctors(1L, false);
    }

    @Test
    @DisplayName("getDoctorById should return doctor user when found")
    void getDoctorById_shouldReturnResponseFromUsersTable() {
        when(userRepository.findByIdAndClinicIdAndRoleAndIsDeletedFalse(10L, 1L, UserRole.DOCTOR))
                .thenReturn(Optional.of(doctorUser));

        DoctorResponse result = doctorManagementService.getDoctorById(10L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getEmail()).isEqualTo("jane.smith@dental.com");
    }

    @Test
    @DisplayName("getDoctorById should throw ResourceNotFoundException when not found in users table")
    void getDoctorById_notFound_shouldThrowException() {
        when(userRepository.findByIdAndClinicIdAndRoleAndIsDeletedFalse(99L, 1L, UserRole.DOCTOR))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorManagementService.getDoctorById(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Doctor not found with id: 99");
    }

    @Test
    @DisplayName("createDoctor should persist doctor user in users table")
    void createDoctor_shouldSucceed() {
        DoctorRequest request = DoctorRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@dental.com")
                .phoneNumber("+1234567890")
                .specialization("General Dentistry")
                .build();

        when(userRepository.existsByUsernameAndIsDeletedFalse(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(doctorUser);
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctorEntity);

        DoctorResponse result = doctorManagementService.createDoctor(request, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateDoctor should update doctor user in users table")
    void updateDoctor_shouldSucceed() {
        DoctorRequest request = DoctorRequest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@dental.com")
                .build();

        when(userRepository.findByIdAndClinicIdAndRoleAndIsDeletedFalse(10L, 1L, UserRole.DOCTOR))
                .thenReturn(Optional.of(doctorUser));
        when(userRepository.save(any(User.class))).thenReturn(doctorUser);
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctorEntity);

        DoctorResponse result = doctorManagementService.updateDoctor(10L, request, 1L);

        assertThat(result).isNotNull();
        verify(userRepository).save(doctorUser);
    }

    @Test
    @DisplayName("deleteDoctor should soft-delete user in users table")
    void deleteDoctor_shouldSoftDeleteInUsersTable() {
        when(userRepository.findByIdAndClinicIdAndRoleAndIsDeletedFalse(10L, 1L, UserRole.DOCTOR))
                .thenReturn(Optional.of(doctorUser));

        doctorManagementService.deleteDoctor(10L, 1L);

        assertThat(doctorUser.getIsDeleted()).isTrue();
        verify(userRepository).save(doctorUser);
    }

    @Test
    @DisplayName("getDoctorAvailability should verify doctor user in users table")
    void getDoctorAvailability_shouldReturnList() {
        DoctorAvailability availability = DoctorAvailability.builder()
                .clinicId(1L)
                .doctor(doctorEntity)
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .isActive(true)
                .build();

        DoctorAvailabilityResponse resp = DoctorAvailabilityResponse.builder()
                .id(1L)
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .isActive(true)
                .build();

        when(userRepository.findByIdAndClinicIdAndRoleAndIsDeletedFalse(10L, 1L, UserRole.DOCTOR))
                .thenReturn(Optional.of(doctorUser));
        when(doctorAvailabilityRepository.findByClinicIdAndDoctorIdAndIsActiveTrueAndIsDeletedFalse(1L, 10L))
                .thenReturn(List.of(availability));
        when(doctorMapper.toAvailabilityResponseList(List.of(availability))).thenReturn(List.of(resp));

        List<DoctorAvailabilityResponse> result = doctorManagementService.getDoctorAvailability(10L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDayOfWeek()).isEqualTo("MONDAY");
    }

    @Test
    @DisplayName("updateDoctorAvailability should update slots for doctor user")
    void updateDoctorAvailability_shouldSucceed() {
        DoctorAvailabilityRequest req = DoctorAvailabilityRequest.builder()
                .dayOfWeek("TUESDAY")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(16, 0))
                .isActive(true)
                .build();

        DoctorAvailability entity = DoctorAvailability.builder()
                .dayOfWeek("TUESDAY")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(16, 0))
                .isActive(true)
                .build();

        DoctorAvailabilityResponse resp = DoctorAvailabilityResponse.builder()
                .dayOfWeek("TUESDAY")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(16, 0))
                .isActive(true)
                .build();

        when(userRepository.findByIdAndClinicIdAndRoleAndIsDeletedFalse(10L, 1L, UserRole.DOCTOR))
                .thenReturn(Optional.of(doctorUser));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctorEntity);
        when(doctorAvailabilityRepository.findByDoctorIdAndIsDeletedFalse(10L)).thenReturn(List.of());
        when(doctorMapper.toAvailabilityEntity(req)).thenReturn(entity);
        when(doctorAvailabilityRepository.saveAll(any())).thenReturn(List.of(entity));
        when(doctorMapper.toAvailabilityResponseList(List.of(entity))).thenReturn(List.of(resp));

        List<DoctorAvailabilityResponse> result = doctorManagementService.updateDoctorAvailability(10L, List.of(req), 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDayOfWeek()).isEqualTo("TUESDAY");
    }
}
