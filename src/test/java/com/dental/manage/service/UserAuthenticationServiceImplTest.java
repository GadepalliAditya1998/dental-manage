package com.dental.manage.service;

import com.dental.manage.dto.request.LoginRequest;
import com.dental.manage.dto.response.LoginResponse;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.User;
import com.dental.manage.enums.UserRole;
import com.dental.manage.exception.AccessDeniedException;
import com.dental.manage.exception.InvalidCredentialsException;
import com.dental.manage.repository.DoctorRepository;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.service.impl.UserAuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JWTtokenService jwtTokenService;

    @InjectMocks
    private UserAuthenticationServiceImpl authenticationService;

    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("authenticate should succeed for RECEPTIONIST with base64 encoded credentials")
    void authenticate_receptionist_shouldSucceed() {
        User receptionist = User.builder()
                .clinicId(1L)
                .username("receptionist")
                .password("reception123")
                .role(UserRole.RECEPTIONIST)
                .fullName("Clinic Receptionist")
                .isActive(true)
                .build();

        when(userRepository.findByUsernameAndIsDeletedFalse("receptionist")).thenReturn(Optional.of(receptionist));
        when(jwtTokenService.createToken(eq("receptionist"), eq("RECEPTIONIST"), any())).thenReturn("mock.jwt.token");
        when(jwtTokenService.getValidityInMilliseconds()).thenReturn(86400000L);

        LoginRequest request = LoginRequest.builder()
                .username(encode("receptionist"))
                .password(encode("reception123"))
                .build();

        LoginResponse response = authenticationService.authenticate(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getRole()).isEqualTo("RECEPTIONIST");
        assertThat(response.getUsername()).isEqualTo("receptionist");
        assertThat(response.getExpiresIn()).isEqualTo(86400L);
    }

    @Test
    @DisplayName("authenticate should succeed for DOCTOR with base64 encoded credentials")
    void authenticate_doctor_shouldSucceed() {
        User doctor = User.builder()
                .clinicId(1L)
                .username("dr.smith")
                .password("doctorPass123")
                .role(UserRole.DOCTOR)
                .fullName("Dr. Jane Smith")
                .isActive(true)
                .build();
        doctor.setId(10L);

        when(userRepository.findByUsernameAndIsDeletedFalse("dr.smith")).thenReturn(Optional.of(doctor));
        when(jwtTokenService.createToken(eq("dr.smith"), eq("DOCTOR"), any())).thenReturn("mock.doctor.token");
        when(jwtTokenService.getValidityInMilliseconds()).thenReturn(86400000L);

        LoginRequest request = LoginRequest.builder()
                .username(encode("dr.smith"))
                .password(encode("doctorPass123"))
                .build();

        LoginResponse response = authenticationService.authenticate(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock.doctor.token");
        assertThat(response.getRole()).isEqualTo("DOCTOR");
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getDoctorId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("authenticate should deny access for PATIENT role with AccessDeniedException")
    void authenticate_patient_shouldThrowAccessDeniedException() {
        User patient = User.builder()
                .clinicId(1L)
                .username("patient_user")
                .password("patientPass")
                .role(UserRole.PATIENT)
                .fullName("John Patient")
                .isActive(true)
                .build();
        patient.setId(99L);

        when(userRepository.findByUsernameAndIsDeletedFalse("patient_user")).thenReturn(Optional.of(patient));

        LoginRequest request = LoginRequest.builder()
                .username(encode("patient_user"))
                .password(encode("patientPass"))
                .build();

        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied. Patients have no access to the portal as of now.");
    }

    @Test
    @DisplayName("authenticate should throw InvalidCredentialsException when password does not match")
    void authenticate_wrongPassword_shouldThrowException() {
        User receptionist = User.builder()
                .username("receptionist")
                .password("reception123")
                .role(UserRole.RECEPTIONIST)
                .isActive(true)
                .build();

        when(userRepository.findByUsernameAndIsDeletedFalse("receptionist")).thenReturn(Optional.of(receptionist));

        LoginRequest request = LoginRequest.builder()
                .username(encode("receptionist"))
                .password(encode("wrongPassword"))
                .build();

        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    @DisplayName("authenticate should throw InvalidCredentialsException when user is not found")
    void authenticate_userNotFound_shouldThrowException() {
        when(userRepository.findByUsernameAndIsDeletedFalse("unknownUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmailAndIsDeletedFalse("unknownUser")).thenReturn(Optional.empty());

        LoginRequest request = LoginRequest.builder()
                .username(encode("unknownUser"))
                .password(encode("anyPassword"))
                .build();

        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    @DisplayName("authenticate should throw InvalidCredentialsException when user account is disabled")
    void authenticate_disabledUser_shouldThrowException() {
        User disabledDoctor = User.builder()
                .username("dr.inactive")
                .password("secret123")
                .role(UserRole.DOCTOR)
                .isActive(false)
                .build();

        when(userRepository.findByUsernameAndIsDeletedFalse("dr.inactive")).thenReturn(Optional.of(disabledDoctor));

        LoginRequest request = LoginRequest.builder()
                .username(encode("dr.inactive"))
                .password(encode("secret123"))
                .build();

        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("User account is disabled");
    }

    @Test
    @DisplayName("authenticate should throw InvalidCredentialsException when credentials are not valid Base64")
    void authenticate_invalidBase64_shouldThrowException() {
        LoginRequest request = LoginRequest.builder()
                .username("not-a-valid-base64-string!@#$")
                .password(encode("password"))
                .build();

        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("must be base64-encoded");
    }
}
