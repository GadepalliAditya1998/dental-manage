package com.dental.manage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicSettingsResponse {

    private Long id;
    private String clinicName;
    private String clinicPhone;
    private String receptionistPhone;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String workingDays;
    private String retellAgentId;
    private String timezone;
}
