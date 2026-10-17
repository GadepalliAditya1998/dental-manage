package com.dental.manage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Daily aggregated metrics for the dashboard.
 */
@Entity
@Table(name = "call_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallMetric extends BaseEntity {

    @Column(name = "metric_date", nullable = false, unique = true)
    private LocalDate metricDate;

    @Column(name = "total_ai_calls")
    @Builder.Default
    private Integer totalAiCalls = 0;

    @Column(name = "total_human_transfers")
    @Builder.Default
    private Integer totalHumanTransfers = 0;

    @Column(name = "successful_bookings")
    @Builder.Default
    private Integer successfulBookings = 0;

    @Column(name = "new_lead_bookings")
    @Builder.Default
    private Integer newLeadBookings = 0;

    @Column(name = "cancelled_appointments")
    @Builder.Default
    private Integer cancelledAppointments = 0;

    @Column(name = "rescheduled_appointments")
    @Builder.Default
    private Integer rescheduledAppointments = 0;

    @Column(name = "avg_call_duration_sec", precision = 10, scale = 2)
    private BigDecimal avgCallDurationSec;

    @Column(name = "avg_sentiment_score", precision = 5, scale = 2)
    private BigDecimal avgSentimentScore;
}
