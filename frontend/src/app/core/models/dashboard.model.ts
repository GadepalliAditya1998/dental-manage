export interface DailyMetric {
  date: string;
  totalAiCalls: number;
  totalHumanTransfers: number;
  successfulBookings: number;
  newLeadBookings: number;
}

export interface DashboardMetricsResponse {
  clinicId?: number;
  totalAiCalls: number;
  totalHumanTransfers: number;
  successfulBookings: number;
  newLeadBookings: number;
  cancelledAppointments: number;
  rescheduledAppointments: number;
  avgCallDurationSec: number;
  avgSentimentScore: number;
  dailyMetrics: DailyMetric[];
}
