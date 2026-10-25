package com.dental.manage.service.impl;

import com.dental.manage.dto.response.CallRecordResponse;
import com.dental.manage.entity.CallRecord;
import com.dental.manage.enums.UserRole;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.mapper.AppointmentMapper;
import com.dental.manage.repository.CallRecordRepository;
import com.dental.manage.repository.UserRepository;
import com.dental.manage.service.CallRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CallRecordServiceImpl implements CallRecordService {

    private final CallRecordRepository callRecordRepository;
    private final UserRepository userRepository;
    private final AppointmentMapper appointmentMapper;

    @Override
    public Page<CallRecordResponse> getCalls(Long clinicId, String callType, Pageable pageable) {
        Page<CallRecord> page = (clinicId != null)
                ? callRecordRepository.findByClinicIdAndIsDeletedFalse(clinicId, pageable)
                : callRecordRepository.findByIsDeletedFalse(pageable);

        return page.map(appointmentMapper::toCallRecordResponse);
    }

    @Override
    public List<CallRecordResponse> getRecentCalls(Long clinicId, Long doctorId, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(limit, 50)));

        List<CallRecord> calls;
        if (doctorId != null) {
            calls = callRecordRepository.findRecentCallsByDoctor(clinicId, doctorId, pageable);
            if (calls.isEmpty()) {
                long activeDoctors = userRepository.countByClinicIdAndRoleAndIsActiveTrueAndIsDeletedFalse(clinicId, UserRole.DOCTOR);
                if (activeDoctors <= 1) {
                    calls = callRecordRepository.findRecentCallsByClinic(clinicId, pageable);
                }
            }
        } else {
            calls = callRecordRepository.findRecentCallsByClinic(clinicId, pageable);
        }

        return calls.stream()
                .map(appointmentMapper::toCallRecordResponse)
                .toList();
    }

    @Override
    public CallRecordResponse getCallById(Long id, Long clinicId) {
        CallRecord record = (clinicId != null)
                ? callRecordRepository.findByIdAndClinicIdAndIsDeletedFalse(id, clinicId)
                        .orElseThrow(() -> new ResourceNotFoundException("CallRecord not found with id: " + id))
                : callRecordRepository.findByIdAndIsDeletedFalse(id)
                        .orElseThrow(() -> new ResourceNotFoundException("CallRecord not found with id: " + id));

        return appointmentMapper.toCallRecordResponse(record);
    }
}
