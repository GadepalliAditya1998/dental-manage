package com.dental.manage.controller;

import com.dental.manage.dto.response.ApiKeyResponse;
import com.dental.manage.exception.GlobalExceptionHandler;
import com.dental.manage.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApiKeyControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ApiKeyService apiKeyService;

    @InjectMocks
    private ApiKeyController apiKeyController;

    private ApiKeyResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(apiKeyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = ApiKeyResponse.builder()
                .id(1L)
                .clinicId(1L)
                .clinicName("Smile Dental Clinic")
                .keyName("Retell AI Voice Agent")
                .apiKey("retell_live_clinic_1_sk_sample_key_2026")
                .agentType("RETELL_AGENT")
                .isActive(true)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/keys/active should return active Retell API key")
    void getActiveRetellApiKey_shouldReturnActiveKey() throws Exception {
        when(apiKeyService.getActiveRetellApiKey(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/keys/active")
                        .param("clinicId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.apiKey", is("retell_live_clinic_1_sk_sample_key_2026")))
                .andExpect(jsonPath("$.data.agentType", is("RETELL_AGENT")))
                .andExpect(jsonPath("$.data.isActive", is(true)));
    }

    @Test
    @DisplayName("POST /api/keys/regenerate should regenerate and return new key")
    void regenerateRetellApiKey_shouldRegenerateKey() throws Exception {
        ApiKeyResponse newResponse = ApiKeyResponse.builder()
                .id(2L)
                .clinicId(1L)
                .clinicName("Smile Dental Clinic")
                .keyName("Retell AI Voice Agent")
                .apiKey("retell_live_new_sample_key_789")
                .agentType("RETELL_AGENT")
                .isActive(true)
                .build();

        when(apiKeyService.createOrRegenerateRetellApiKey(eq(1L), eq("Retell AI Voice Agent")))
                .thenReturn(newResponse);

        mockMvc.perform(post("/api/keys/regenerate")
                        .param("clinicId", "1")
                        .param("name", "Retell AI Voice Agent")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.apiKey", is("retell_live_new_sample_key_789")));
    }

    @Test
    @DisplayName("GET /api/keys/all should return list of clinic keys")
    void getAllClinicApiKeys_shouldReturnList() throws Exception {
        when(apiKeyService.getClinicApiKeys(1L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/keys/all")
                        .param("clinicId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].agentType", is("RETELL_AGENT")));
    }

    @Test
    @DisplayName("POST /api/keys/{id}/deactivate should deactivate key")
    void deactivateApiKey_shouldDeactivate() throws Exception {
        mockMvc.perform(post("/api/keys/1/deactivate")
                        .param("clinicId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(apiKeyService).deactivateApiKey(1L, 1L);
    }

    @Test
    @DisplayName("DELETE /api/keys/{id} should soft delete key")
    void deleteApiKey_shouldDelete() throws Exception {
        mockMvc.perform(delete("/api/keys/1")
                        .param("clinicId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(apiKeyService).deleteApiKey(1L, 1L);
    }
}
