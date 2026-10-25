package com.dental.manage.controller;

import com.dental.manage.dto.response.CallRecordResponse;
import com.dental.manage.exception.GlobalExceptionHandler;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.service.CallRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CallRecordControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CallRecordService callRecordService;

    @InjectMocks
    private CallRecordController callRecordController;

    private CallRecordResponse sampleCall;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(callRecordController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleCall = CallRecordResponse.builder()
                .id(1L)
                .clinicId(1L)
                .callerPhone("+1234567890")
                .callType("AI_INBOUND")
                .callStatus("COMPLETED")
                .durationMs(120000)
                .callSummary("Patient asked for teeth scaling appointment")
                .sentiment("POSITIVE")
                .transferredToHuman(false)
                .build();
    }

    @Test
    @DisplayName("GET /api/calls should return page of call records")
    void getCalls_shouldReturnSuccess() throws Exception {
        when(callRecordService.getCalls(eq(1L), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleCall), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/calls")
                        .param("clinicId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].callerPhone", is("+1234567890")));
    }

    @Test
    @DisplayName("GET /api/calls/recent should return recent call list")
    void getRecentCalls_shouldReturnSuccess() throws Exception {
        when(callRecordService.getRecentCalls(eq(1L), eq(null), eq(5)))
                .thenReturn(List.of(sampleCall));

        mockMvc.perform(get("/api/calls/recent")
                        .param("clinicId", "1")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].callSummary", is("Patient asked for teeth scaling appointment")));
    }

    @Test
    @DisplayName("GET /api/calls/{id} should return call record when found")
    void getCallById_shouldReturnSuccess() throws Exception {
        when(callRecordService.getCallById(eq(1L), eq(1L))).thenReturn(sampleCall);

        mockMvc.perform(get("/api/calls/1")
                        .param("clinicId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)));
    }

    @Test
    @DisplayName("GET /api/calls/{id} should return 404 when not found")
    void getCallById_notFound_shouldReturn404() throws Exception {
        when(callRecordService.getCallById(eq(99L), eq(1L)))
                .thenThrow(new ResourceNotFoundException("CallRecord not found with id: 99"));

        mockMvc.perform(get("/api/calls/99")
                        .param("clinicId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
