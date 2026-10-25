package com.dental.manage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard metrics aggregation for receptionist and doctor portals.
 * Supports filtering by DAY, WEEK, MONTH, and CUSTOM date ranges.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMetricsResponse {

    private Long clinicId;
    private Long doctorId;
    private String filter;
    private String startDate;
    private String endDate;

    /** Total calls received by the practitioner or clinic */
    private long callsReceived;

    /** Appointments booked via AI Call Agent */
    private long appointmentsBookedViaCallAgent;

    /** Total clinic calls received (useful when doctor view is scoped) */
    private long clinicTotalCalls;

    /** Legacy & detailed breakdown metrics */
    private long totalAiCalls;
    private long totalHumanTransfers;
    private long successfulBookings;
    private long newLeadBookings;
    private long cancelledAppointments;
    private long rescheduledAppointments;
    private BigDecimal avgCallDurationSec;
    private BigDecimal avgSentimentScore;

    /** Daily breakdown for charts and trend lines */
    private List<DailyMetric> dailyMetrics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyMetric {
        private String date;
        private int callsReceived;
        private int appointmentsBookedViaCallAgent;
        private int totalAiCalls;
        private int totalHumanTransfers;
        private int successfulBookings;
        private int newLeadBookings;
    }
}
