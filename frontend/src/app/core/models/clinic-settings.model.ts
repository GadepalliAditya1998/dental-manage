export interface ClinicSettingsResponse {
  id: number;
  clinicId?: number;
  clinicName: string;
  clinicPhone?: string;
  receptionistPhone?: string;
  openingTime?: string; // "09:00:00"
  closingTime?: string; // "18:00:00"
  workingDays?: string; // e.g. "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY"
  retellAgentId?: string;
  timezone?: string;
}

export interface ClinicSettingsRequest {
  clinicId?: number;
  clinicName: string;
  clinicPhone?: string;
  receptionistPhone?: string;
  openingTime?: string;
  closingTime?: string;
  workingDays?: string;
  retellAgentId?: string;
  timezone?: string;
}
