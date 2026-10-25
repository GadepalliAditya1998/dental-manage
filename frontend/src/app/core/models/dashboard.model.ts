export interface DailyMetric {
  date: string;
  callsReceived?: number;
  appointmentsBookedViaCallAgent?: number;
  totalAiCalls: number;
  totalHumanTransfers: number;
  successfulBookings: number;
  newLeadBookings: number;
}

export interface DashboardMetricsResponse {
  clinicId?: number;
  doctorId?: number;
  filter?: 'DAY' | 'WEEK' | 'MONTH' | 'CUSTOM' | string;
  startDate?: string;
  endDate?: string;
  callsReceived: number;
  appointmentsBookedViaCallAgent: number;
  clinicTotalCalls?: number;
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
