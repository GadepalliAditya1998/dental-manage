import { PatientResponse } from './patient.model';
import { DoctorResponse } from './doctor.model';
import { ServiceResponse } from './service.model';
import { CallRecordResponse } from './call-record.model';

export type AppointmentStatus =
  | 'SCHEDULED'
  | 'REMINDER_SENT'
  | 'CONFIRMED'
  | 'RESCHEDULED'
  | 'CANCELLED'
  | 'NO_SHOW'
  | 'COMPLETED';

export type ConfirmationStatus = 'PENDING' | 'CONFIRMED' | 'DECLINED';

export type BookingSource = 'MANUAL' | 'AI_AGENT';

export interface AppointmentResponse {
  id: number;
  clinicId?: number;
  patient: PatientResponse;
  doctor: DoctorResponse;
  service: ServiceResponse;
  appointmentStart: string; // ISO date-time string
  appointmentEnd: string;
  reason?: string;
  startTime?: string;
  endTime?: string;
  reasonForAppointment?: string;
  status: AppointmentStatus;
  bookingSource?: BookingSource;
  calendarEventAdded?: boolean;
  calendarEventId?: string;
  confirmationStatus?: ConfirmationStatus;
  reminderSentAt?: string;
  createdOn?: string;
  callRecord?: CallRecordResponse;
}

export interface AppointmentRequest {
  clinicId?: number;
  patientId: number;
  doctorId: number;
  serviceId: number;
  appointmentDateTime?: string; // ISO LocalDateTime string e.g. "2026-10-20T10:00:00"
  startTime?: string;
  endTime?: string;
  reason?: string;
  reasonForAppointment?: string;
  callRecordId?: number;
  bookingSource?: BookingSource;
}

export interface CalendarEventRequest {
  appointmentId: number;
  patientEmail?: string;
}
