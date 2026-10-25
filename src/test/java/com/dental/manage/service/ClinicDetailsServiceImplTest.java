package com.dental.manage.service;

import com.dental.manage.dto.request.ClinicSettingsRequest;
import com.dental.manage.dto.response.ApiKeyResponse;
import com.dental.manage.dto.response.ClinicSettingsResponse;
import com.dental.manage.dto.response.ClinicWorkingHoursResponse;
import com.dental.manage.entity.ClinicDetails;
import com.dental.manage.exception.ResourceNotFoundException;
import com.dental.manage.mapper.ClinicDetailsMapper;
import com.dental.manage.repository.ClinicDetailsRepository;
import com.dental.manage.service.impl.ClinicDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicDetailsServiceImplTest {

    @Mock
    private ClinicDetailsRepository clinicDetailsRepository;

    @Mock
    private ClinicDetailsMapper clinicDetailsMapper;

    @Mock
    private ApiKeyService apiKeyService;

    @InjectMocks
    private ClinicDetailsServiceImpl clinicDetailsService;

    private ClinicDetails clinicDetails;
    private ClinicWorkingHoursResponse baseResponse;

    @BeforeEach
    void setUp() {
        clinicDetails = ClinicDetails.builder()
                .clinicName("Smile Dental Clinic")
                .clinicPhone("+1234567890")
                .receptionistPhone("+1987654321")
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(18, 0))
                .workingDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY")
                .timezone("Asia/Kolkata")
                .retellAgentId("agent_123")
                .build();
        clinicDetails.setId(1L);

        baseResponse = ClinicWorkingHoursResponse.builder()
                .id(1L)
                .clinicName("Smile Dental Clinic")
                .clinicPhone("+1234567890")
                .receptionistPhone("+1987654321")
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(18, 0))
                .workingDays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY")
                .timezone("Asia/Kolkata")
                .retellAgentId("agent_123")
                .build();
    }

    @Test
    @DisplayName("getClinicDetailsWithWorkingHours should return 7 day-wise records for a week")
    void getClinicDetails_shouldReturnWeekWorkingHours() {
        when(clinicDetailsRepository.findFirstByIsDeletedFalseOrderByIdAsc()).thenReturn(Optional.of(clinicDetails));
        when(clinicDetailsMapper.toWorkingHoursResponse(clinicDetails)).thenReturn(baseResponse);

        // 2026-10-19 is a Monday
        LocalDateTime targetTime = LocalDateTime.of(2026, 10, 19, 10, 30);
        ClinicWorkingHoursResponse result = clinicDetailsService.getClinicDetailsWithWorkingHours("week", targetTime, null);

        assertThat(result).isNotNull();
        assertThat(result.getPeriod()).isEqualTo("WEEK");
        List<ClinicWorkingHoursResponse.DayWorkingHours> hours = result.getWorkingHours();
        assertThat(hours).hasSize(7);

        // Verify Monday (open)
        ClinicWorkingHoursResponse.DayWorkingHours monday = hours.get(0);
        assertThat(monday.getDayOfWeek()).isEqualTo("MONDAY");
        assertThat(monday.getIsOpen()).isTrue();
        assertThat(monday.getStatus()).isEqualTo("OPEN");
        assertThat(monday.getOpeningTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(monday.getClosingTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(monday.getDateTime()).isEqualTo(LocalDateTime.of(2026, 10, 19, 0, 0));
        assertThat(monday.getFormattedHours()).contains("09:00 AM - 06:00 PM");

        // Verify Saturday (open)
        ClinicWorkingHoursResponse.DayWorkingHours saturday = hours.get(5);
        assertThat(saturday.getDayOfWeek()).isEqualTo("SATURDAY");
        assertThat(saturday.getIsOpen()).isTrue();

        // Verify Sunday (closed)
        ClinicWorkingHoursResponse.DayWorkingHours sunday = hours.get(6);
        assertThat(sunday.getDayOfWeek()).isEqualTo("SUNDAY");
        assertThat(sunday.getIsOpen()).isFalse();
        assertThat(sunday.getStatus()).isEqualTo("CLOSED");
        assertThat(sunday.getOpeningTime()).isNull();
        assertThat(sunday.getClosingTime()).isNull();
        assertThat(sunday.getFormattedHours()).isEqualTo("Closed");
    }

    @Test
    @DisplayName("getClinicDetailsWithWorkingHours should return single day-wise record when period is DAY")
    void getClinicDetails_shouldReturnDayWorkingHours() {
        when(clinicDetailsRepository.findFirstByIsDeletedFalseOrderByIdAsc()).thenReturn(Optional.of(clinicDetails));
        when(clinicDetailsMapper.toWorkingHoursResponse(clinicDetails)).thenReturn(baseResponse);

        // Monday 2026-10-19
        LocalDateTime targetTime = LocalDateTime.of(2026, 10, 19, 14, 0);
        ClinicWorkingHoursResponse result = clinicDetailsService.getClinicDetailsWithWorkingHours("DAY", targetTime, null);

        assertThat(result).isNotNull();
        assertThat(result.getPeriod()).isEqualTo("DAY");
        assertThat(result.getWorkingHours()).hasSize(1);

        ClinicWorkingHoursResponse.DayWorkingHours dayRecord = result.getWorkingHours().get(0);
        assertThat(dayRecord.getDayOfWeek()).isEqualTo("MONDAY");
        assertThat(dayRecord.getDateTime()).isEqualTo(LocalDateTime.of(2026, 10, 19, 0, 0));
        assertThat(dayRecord.getIsOpen()).isTrue();
        assertThat(dayRecord.getStatus()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("getClinicDetailsWithWorkingHours should resolve specific dayOfWeek parameter")
    void getClinicDetails_shouldResolveSpecificDayOfWeek() {
        when(clinicDetailsRepository.findFirstByIsDeletedFalseOrderByIdAsc()).thenReturn(Optional.of(clinicDetails));
        when(clinicDetailsMapper.toWorkingHoursResponse(clinicDetails)).thenReturn(baseResponse);

        // Base date is Monday 2026-10-19, request SUNDAY
        LocalDateTime targetTime = LocalDateTime.of(2026, 10, 19, 10, 0);
        ClinicWorkingHoursResponse result = clinicDetailsService.getClinicDetailsWithWorkingHours("DAY", targetTime, "SUNDAY");

        assertThat(result).isNotNull();
        assertThat(result.getWorkingHours()).hasSize(1);
        ClinicWorkingHoursResponse.DayWorkingHours dayRecord = result.getWorkingHours().get(0);
        assertThat(dayRecord.getDayOfWeek()).isEqualTo("SUNDAY");
        assertThat(dayRecord.getDateTime()).isEqualTo(LocalDateTime.of(2026, 10, 25, 0, 0));
        assertThat(dayRecord.getIsOpen()).isFalse();
        assertThat(dayRecord.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("getClinicDetailsWithWorkingHours should throw IllegalArgumentException for invalid period")
    void getClinicDetails_shouldThrowExceptionForInvalidPeriod() {
        when(clinicDetailsRepository.findFirstByIsDeletedFalseOrderByIdAsc()).thenReturn(Optional.of(clinicDetails));
        when(clinicDetailsMapper.toWorkingHoursResponse(clinicDetails)).thenReturn(baseResponse);

        assertThatThrownBy(() -> clinicDetailsService.getClinicDetailsWithWorkingHours("MONTH", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid period 'MONTH'");
    }

    @Test
    @DisplayName("getClinicDetailsWithWorkingHours should throw ResourceNotFoundException when clinic record is absent")
    void getClinicDetails_shouldThrowWhenNotFound() {
        when(clinicDetailsRepository.findFirstByIsDeletedFalseOrderByIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clinicDetailsService.getClinicDetailsWithWorkingHours("WEEK", null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Clinic details configuration not found");
    }

    @Test
    @DisplayName("updateClinicDetails should update entity and return settings response")
    void updateClinicDetails_shouldUpdateAndReturnSettings() {
        ClinicSettingsRequest request = ClinicSettingsRequest.builder()
                .clinicName("Updated Clinic")
                .clinicPhone("+9999999999")
                .build();

        ClinicSettingsResponse response = ClinicSettingsResponse.builder()
                .id(1L)
                .clinicName("Updated Clinic")
                .clinicPhone("+9999999999")
                .build();

        when(clinicDetailsRepository.findFirstByIsDeletedFalseOrderByIdAsc()).thenReturn(Optional.of(clinicDetails));
        when(clinicDetailsRepository.save(any(ClinicDetails.class))).thenReturn(clinicDetails);
        when(clinicDetailsMapper.toSettingsResponse(clinicDetails)).thenReturn(response);

        ClinicSettingsResponse result = clinicDetailsService.updateClinicDetails(request);

        assertThat(result).isNotNull();
        assertThat(result.getClinicName()).isEqualTo("Updated Clinic");
        verify(clinicDetailsMapper).updateEntityFromRequest(request, clinicDetails);
        verify(clinicDetailsRepository).save(clinicDetails);
    }

    @Test
    @DisplayName("getApiKey should delegate to ApiKeyService")
    void getApiKey_shouldDelegateToApiKeyService() {
        ApiKeyResponse mockResponse = ApiKeyResponse.builder()
                .clinicId(1L)
                .clinicName("Smile Dental Clinic")
                .apiKey("retell_live_clinic_1_sk_sample_key_2026")
                .agentType("RETELL_AGENT")
                .isActive(true)
                .build();

        when(apiKeyService.getActiveRetellApiKey(1L)).thenReturn(mockResponse);

        ApiKeyResponse result = clinicDetailsService.getApiKey(1L);

        assertThat(result).isNotNull();
        assertThat(result.getApiKey()).isEqualTo("retell_live_clinic_1_sk_sample_key_2026");
        verify(apiKeyService).getActiveRetellApiKey(1L);
    }

    @Test
    @DisplayName("regenerateApiKey should delegate to ApiKeyService")
    void regenerateApiKey_shouldDelegateToApiKeyService() {
        ApiKeyResponse mockResponse = ApiKeyResponse.builder()
                .clinicId(1L)
                .clinicName("Smile Dental Clinic")
                .apiKey("retell_live_new_key_12345")
                .agentType("RETELL_AGENT")
                .isActive(true)
                .build();

        when(apiKeyService.createOrRegenerateRetellApiKey(1L, null)).thenReturn(mockResponse);

        ApiKeyResponse result = clinicDetailsService.regenerateApiKey(1L);

        assertThat(result).isNotNull();
        assertThat(result.getApiKey()).isEqualTo("retell_live_new_key_12345");
        verify(apiKeyService).createOrRegenerateRetellApiKey(1L, null);
    }
}
