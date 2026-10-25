package com.dental.manage.service;

import com.dental.manage.dto.response.CallRecordResponse;
import com.dental.manage.entity.CallRecord;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.mapper.AppointmentMapper;
import com.dental.manage.enums.UserRole;
import com.dental.manage.repository.CallRecordRepository;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.service.impl.CallRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallRecordServiceImplTest {

    @Mock
    private CallRecordRepository callRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @InjectMocks
    private CallRecordServiceImpl callRecordService;

    private CallRecord sampleEntity;
    private CallRecordResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleEntity = CallRecord.builder()
                .clinicId(1L)
                .callerPhone("+1234567890")
                .callSummary("General consultation")
                .build();
        sampleEntity.setId(1L);

        sampleResponse = CallRecordResponse.builder()
                .id(1L)
                .clinicId(1L)
                .callerPhone("+1234567890")
                .callSummary("General consultation")
                .build();
    }

    @Test
    @DisplayName("getCalls should return paged responses")
    void getCalls_shouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 10);
        when(callRecordRepository.findByClinicIdAndIsDeletedFalse(eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sampleEntity), pageable, 1));
        when(appointmentMapper.toCallRecordResponse(sampleEntity)).thenReturn(sampleResponse);

        Page<CallRecordResponse> result = callRecordService.getCalls(1L, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCallerPhone()).isEqualTo("+1234567890");
    }

    @Test
    @DisplayName("getRecentCalls for clinic should return recent list")
    void getRecentCalls_clinic_shouldReturnList() {
        when(callRecordRepository.findRecentCallsByClinic(eq(1L), any()))
                .thenReturn(List.of(sampleEntity));
        when(appointmentMapper.toCallRecordResponse(sampleEntity)).thenReturn(sampleResponse);

        List<CallRecordResponse> result = callRecordService.getRecentCalls(1L, null, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getRecentCalls for doctor with sole practitioner fallback should return clinic calls")
    void getRecentCalls_doctor_fallback_shouldReturnClinicCalls() {
        when(callRecordRepository.findRecentCallsByDoctor(eq(1L), eq(10L), any()))
                .thenReturn(List.of());
        when(userRepository.countByClinicIdAndRoleAndIsActiveTrueAndIsDeletedFalse(1L, UserRole.DOCTOR))
                .thenReturn(1L);
        when(callRecordRepository.findRecentCallsByClinic(eq(1L), any()))
                .thenReturn(List.of(sampleEntity));
        when(appointmentMapper.toCallRecordResponse(sampleEntity)).thenReturn(sampleResponse);

        List<CallRecordResponse> result = callRecordService.getRecentCalls(1L, 10L, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getCallById should return response when found")
    void getCallById_shouldReturnResponse() {
        when(callRecordRepository.findByIdAndClinicIdAndIsDeletedFalse(1L, 1L))
                .thenReturn(Optional.of(sampleEntity));
        when(appointmentMapper.toCallRecordResponse(sampleEntity)).thenReturn(sampleResponse);

        CallRecordResponse result = callRecordService.getCallById(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getCallById should throw ResourceNotFoundException when not found")
    void getCallById_notFound_shouldThrowException() {
        when(callRecordRepository.findByIdAndClinicIdAndIsDeletedFalse(99L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> callRecordService.getCallById(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CallRecord not found with id: 99");
    }
}
