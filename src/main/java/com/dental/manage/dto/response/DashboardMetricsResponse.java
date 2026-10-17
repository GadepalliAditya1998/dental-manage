package com.dental.manage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard metrics aggregation for the admin panel.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMetricsResponse {

    private long totalAiCalls;
    private long totalHumanTransfers;
    private long successfulBookings;
    private long newLeadBookings;
    private long cancelledAppointments;
    private long rescheduledAppointments;
    private BigDecimal avgCallDurationSec;
    private BigDecimal avgSentimentScore;

    /** Daily breakdown for charts */
    private List<DailyMetric> dailyMetrics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyMetric {
        private String date;
        private int totalAiCalls;
        private int totalHumanTransfers;
        private int successfulBookings;
        private int newLeadBookings;
    }
}
