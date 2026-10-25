import { AppointmentStatus, ConfirmationStatus } from './appointment.model';

export interface PatientResponse {
  id: number;
  patientId?: string;
  clinicId?: number;
  firstName: string;
  lastName?: string;
  fullName?: string;
  username?: string;
  password?: string;
  email?: string;
  phoneNumber: string;
  dateOfBirth?: string;
  isNewLead?: boolean;
  whatsappOptOut?: boolean;
  createdOn?: string;
  modifiedOn?: string;
}

export interface PatientRequest {
  clinicId?: number;
  patientId?: string;
  firstName?: string;
  lastName?: string;
  name?: string;
  email: string;
  phoneNumber: string;
  dateOfBirth?: string;
  isNewLead?: boolean;
  whatsappOptOut?: boolean;
}

export interface PatientLookupRequest {
  patientId?: string;
  phoneNumber?: string;
  mobileNumber?: string;
  fullName?: string;
  dateOfBirth?: string;
  clinicId?: number;
}

export interface PatientLookupResponse {
  id: number;
  patientId: string;
  clinicId?: number;
  username?: string;
  fullName?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  role?: string;
  isActive?: boolean;
  createdOn?: string;
  modifiedOn?: string;
}

export interface PatientUpcomingAppointmentResponse {
  appointmentId: number;
  patientId: number;
  patientName: string;
  patientPhone: string;
  doctorId?: number;
  doctorName?: string;
  doctorSpecialization?: string;
  serviceId?: number;
  serviceName?: string;
  serviceDurationMinutes?: number;
  servicePrice?: number;
  appointmentStart: string;
  appointmentEnd: string;
  status: AppointmentStatus;
  confirmationStatus?: ConfirmationStatus;
}
