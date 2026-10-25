package com.dental.manage.service;

import com.dental.manage.dto.response.ApiKeyResponse;
import com.dental.manage.entity.ApiKey;
import com.dental.manage.entity.ClinicDetails;
import com.dental.manage.exception.InvalidCredentialsException;
import com.dental.manage.repository.ApiKeyRepository;
import com.dental.manage.repository.ClinicDetailsRepository;
import com.dental.manage.service.impl.ApiKeyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ClinicDetailsRepository clinicDetailsRepository;

    @InjectMocks
    private ApiKeyServiceImpl apiKeyService;

    private ClinicDetails clinic;
    private ApiKey sampleRetellKey;

    @BeforeEach
    void setUp() {
        clinic = ClinicDetails.builder()
                .clinicId(1L)
                .clinicName("Smile Dental Clinic")
                .build();
        clinic.setId(1L);

        sampleRetellKey = ApiKey.builder()
                .clinicId(1L)
                .keyName("Retell AI Voice Agent")
                .apiKey("retell_live_clinic_1_sk_sample_key_2026")
                .agentType("RETELL_AGENT")
                .isActive(true)
                .build();
        sampleRetellKey.setId(10L);
    }

    @Test
    @DisplayName("validateAndAuthenticateKey should succeed for active Retell agent key")
    void validateAndAuthenticateKey_shouldSucceedForValidRetellKey() {
        when(apiKeyRepository.findByApiKeyAndIsDeletedFalse("retell_live_clinic_1_sk_sample_key_2026"))
                .thenReturn(Optional.of(sampleRetellKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiKey authenticated = apiKeyService.validateAndAuthenticateKey(
                "retell_live_clinic_1_sk_sample_key_2026", "RETELL_AGENT");

        assertThat(authenticated).isNotNull();
        assertThat(authenticated.getApiKey()).isEqualTo("retell_live_clinic_1_sk_sample_key_2026");
        assertThat(authenticated.isRetellAgentKey()).isTrue();
        assertThat(authenticated.getLastUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("validateAndAuthenticateKey should throw InvalidCredentialsException when key is blank")
    void validateAndAuthenticateKey_shouldThrowWhenBlank() {
        assertThatThrownBy(() -> apiKeyService.validateAndAuthenticateKey("   ", null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("validateAndAuthenticateKey should throw InvalidCredentialsException when key not found")
    void validateAndAuthenticateKey_shouldThrowWhenNotFound() {
        when(apiKeyRepository.findByApiKeyAndIsDeletedFalse("unknown_key"))
                .thenReturn(Optional.empty());
        when(clinicDetailsRepository.findByApiKeyAndIsDeletedFalse("unknown_key"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.validateAndAuthenticateKey("unknown_key", null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid or inactive API key");
    }

    @Test
    @DisplayName("validateAndAuthenticateKey should throw InvalidCredentialsException when key is inactive")
    void validateAndAuthenticateKey_shouldThrowWhenInactive() {
        sampleRetellKey.setIsActive(false);
        when(apiKeyRepository.findByApiKeyAndIsDeletedFalse("retell_live_clinic_1_sk_sample_key_2026"))
                .thenReturn(Optional.of(sampleRetellKey));

        assertThatThrownBy(() -> apiKeyService.validateAndAuthenticateKey("retell_live_clinic_1_sk_sample_key_2026", null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("validateAndAuthenticateKey should throw InvalidCredentialsException when key is expired")
    void validateAndAuthenticateKey_shouldThrowWhenExpired() {
        sampleRetellKey.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(apiKeyRepository.findByApiKeyAndIsDeletedFalse("retell_live_clinic_1_sk_sample_key_2026"))
                .thenReturn(Optional.of(sampleRetellKey));

        assertThatThrownBy(() -> apiKeyService.validateAndAuthenticateKey("retell_live_clinic_1_sk_sample_key_2026", null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("validateAndAuthenticateKey should throw SecurityException when key is not for Retell agent")
    void validateAndAuthenticateKey_shouldThrowWhenNotRetellAgent() {
        sampleRetellKey.setAgentType("OTHER_AGENT");
        when(apiKeyRepository.findByApiKeyAndIsDeletedFalse("retell_live_clinic_1_sk_sample_key_2026"))
                .thenReturn(Optional.of(sampleRetellKey));

        assertThatThrownBy(() -> apiKeyService.validateAndAuthenticateKey("retell_live_clinic_1_sk_sample_key_2026", null))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("allowed only for Retell agent");
    }

    @Test
    @DisplayName("validateAndAuthenticateKey should throw SecurityException when caller is not Retell agent")
    void validateAndAuthenticateKey_shouldThrowWhenCallerIsNotRetellAgent() {
        when(apiKeyRepository.findByApiKeyAndIsDeletedFalse("retell_live_clinic_1_sk_sample_key_2026"))
                .thenReturn(Optional.of(sampleRetellKey));

        assertThatThrownBy(() -> apiKeyService.validateAndAuthenticateKey("retell_live_clinic_1_sk_sample_key_2026", "PATIENT_PORTAL"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("allowed only for Retell agent");
    }

    @Test
    @DisplayName("getActiveRetellApiKey should return existing active Retell key")
    void getActiveRetellApiKey_shouldReturnExistingActiveKey() {
        when(clinicDetailsRepository.findByClinicIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(clinic));
        when(apiKeyRepository.findFirstByClinicIdAndAgentTypeAndIsActiveTrueAndIsDeletedFalseOrderByCreatedOnDesc(1L, "RETELL_AGENT"))
                .thenReturn(Optional.of(sampleRetellKey));

        ApiKeyResponse response = apiKeyService.getActiveRetellApiKey(1L);

        assertThat(response).isNotNull();
        assertThat(response.getApiKey()).isEqualTo("retell_live_clinic_1_sk_sample_key_2026");
        assertThat(response.getAgentType()).isEqualTo("RETELL_AGENT");
    }

    @Test
    @DisplayName("createOrRegenerateRetellApiKey should deactivate prior keys and save new key")
    void createOrRegenerateRetellApiKey_shouldCreateNewKey() {
        when(clinicDetailsRepository.findByClinicIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(clinic));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
            ApiKey k = inv.getArgument(0);
            k.setId(20L);
            return k;
        });

        ApiKeyResponse response = apiKeyService.createOrRegenerateRetellApiKey(1L, "Custom Retell Key");

        assertThat(response).isNotNull();
        assertThat(response.getApiKey()).startsWith("retell_live_");
        assertThat(response.getKeyName()).isEqualTo("Custom Retell Key");
        assertThat(response.getAgentType()).isEqualTo("RETELL_AGENT");

        verify(apiKeyRepository).deactivateAllForClinicAndAgent(1L, "RETELL_AGENT");
        verify(clinicDetailsRepository).save(clinic);
    }

    @Test
    @DisplayName("getClinicApiKeys should return list of clinic keys")
    void getClinicApiKeys_shouldReturnList() {
        when(clinicDetailsRepository.findByClinicIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(clinic));
        when(apiKeyRepository.findByClinicIdAndIsDeletedFalseOrderByCreatedOnDesc(1L))
                .thenReturn(List.of(sampleRetellKey));

        List<ApiKeyResponse> list = apiKeyService.getClinicApiKeys(1L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getApiKey()).isEqualTo("retell_live_clinic_1_sk_sample_key_2026");
    }
}
