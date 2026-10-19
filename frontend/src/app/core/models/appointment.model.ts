import { PatientResponse } from './patient.model';
import { DoctorResponse } from './doctor.model';
import { ServiceResponse } from './service.model';

export type AppointmentStatus =
  | 'SCHEDULED'
  | 'REMINDER_SENT'
  | 'CONFIRMED'
  | 'RESCHEDULED'
  | 'CANCELLED'
  | 'NO_SHOW'
  | 'COMPLETED';

export type ConfirmationStatus = 'PENDING' | 'CONFIRMED' | 'DECLINED';

export interface AppointmentResponse {
  id: number;
  clinicId?: number;
  patient: PatientResponse;
  doctor: DoctorResponse;
  service: ServiceResponse;
  appointmentStart: string; // ISO date-time string
  appointmentEnd: string;
  status: AppointmentStatus;
  calendarEventAdded?: boolean;
  confirmationStatus?: ConfirmationStatus;
  reminderSentAt?: string;
  createdOn?: string;
}

export interface AppointmentRequest {
  clinicId?: number;
  patientId: number;
  doctorId: number;
  serviceId: number;
  appointmentDateTime: string; // ISO LocalDateTime string e.g. "2026-10-20T10:00:00"
  callRecordId?: number;
}

export interface CalendarEventRequest {
  appointmentId: number;
  patientEmail?: string;
}
