package com.dental.manage.controller;

import com.dental.manage.context.ClinicRequestContext;
import com.dental.manage.dto.response.ApiResponse;
import com.dental.manage.dto.response.CallRecordResponse;
import com.dental.manage.service.CallRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/calls", "/api/clinics/{clinicId}/calls"})
@RequiredArgsConstructor
@Slf4j
public class CallRecordController {

    private final CallRecordService callRecordService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CallRecordResponse>>> getCalls(
            @RequestParam(name = "callType", required = false) String callType,
            @RequestParam(name = "clinicId", required = false) Long clinicId,
            @PageableDefault(size = 20) Pageable pageable) {

        Long effectiveClinicId = clinicId != null ? clinicId : ClinicRequestContext.getClinicId();
        Page<CallRecordResponse> response = callRecordService.getCalls(effectiveClinicId, callType, pageable);
        return ResponseEntity.ok(ApiResponse.success("Calls retrieved successfully", response));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<CallRecordResponse>>> getRecentCalls(
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "clinicId", required = false) Long clinicId) {

        Long effectiveClinicId = clinicId != null ? clinicId : ClinicRequestContext.getClinicId();
        List<CallRecordResponse> response = callRecordService.getRecentCalls(effectiveClinicId, doctorId, limit);
        return ResponseEntity.ok(ApiResponse.success("Recent calls retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CallRecordResponse>> getCallById(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "clinicId", required = false) Long clinicId) {

        Long effectiveClinicId = clinicId != null ? clinicId : ClinicRequestContext.getClinicId();
        CallRecordResponse response = callRecordService.getCallById(id, effectiveClinicId);
        return ResponseEntity.ok(ApiResponse.success("Call details retrieved successfully", response));
    }
}
