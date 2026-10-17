package com.dental.manage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallRecordResponse {

    private Long id;
    private String retellCallId;
    private PatientResponse patient;
    private String callType;
    private String callStatus;
    private String callerPhone;
    private Integer durationMs;
    private String transcript;
    private String recordingUrl;
    private String sentiment;
    private BigDecimal sentimentScore;
    private String callSummary;
    private Boolean transferredToHuman;
    private String humanCallRecordingUrl;
    private String humanCallTranscript;
    private LocalDateTime callStartedAt;
    private LocalDateTime callEndedAt;
    private LocalDateTime createdOn;
}
