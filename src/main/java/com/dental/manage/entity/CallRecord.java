package com.dental.manage.entity;

import com.dental.manage.enums.CallType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records every call handled by the system (AI or human-transferred).
 * Stores transcript, recording URL, sentiment analysis, and transfer details.
 */
@Entity
@Table(name = "call_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallRecord extends BaseEntity {

    @Column(name = "retell_call_id", unique = true)
    private String retellCallId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false, length = 20)
    private CallType callType;

    @Column(name = "call_status", length = 20)
    private String callStatus;

    @Column(name = "caller_phone", length = 20)
    private String callerPhone;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "transcript_object", columnDefinition = "JSONB")
    private String transcriptObject;

    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    @Column(name = "sentiment", length = 20)
    private String sentiment;

    @Column(name = "sentiment_score", precision = 5, scale = 2)
    private BigDecimal sentimentScore;

    @Column(name = "call_summary", columnDefinition = "TEXT")
    private String callSummary;

    @Column(name = "transferred_to_human")
    @Builder.Default
    private Boolean transferredToHuman = false;

    @Column(name = "human_call_recording_url", length = 500)
    private String humanCallRecordingUrl;

    @Column(name = "human_call_transcript", columnDefinition = "TEXT")
    private String humanCallTranscript;

    @Column(name = "call_started_at")
    private LocalDateTime callStartedAt;

    @Column(name = "call_ended_at")
    private LocalDateTime callEndedAt;
}
