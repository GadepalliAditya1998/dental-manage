package com.dental.manage.service;

import com.dental.manage.dto.response.CallRecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CallRecordService {

    Page<CallRecordResponse> getCalls(Long clinicId, String callType, Pageable pageable);

    List<CallRecordResponse> getRecentCalls(Long clinicId, Long doctorId, int limit);

    CallRecordResponse getCallById(Long id, Long clinicId);
}
