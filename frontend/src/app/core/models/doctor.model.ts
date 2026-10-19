export interface DoctorResponse {
  id: number;
  clinicId?: number;
  firstName: string;
  lastName: string;
  specialization?: string;
  email?: string;
  phoneNumber?: string;
  isActive?: boolean;
  createdOn?: string;
}

export interface DoctorRequest {
  clinicId?: number;
  firstName: string;
  lastName: string;
  specialization?: string;
  email?: string;
  phoneNumber?: string;
  isActive?: boolean;
}

export interface DoctorAvailabilityResponse {
  id: number;
  clinicId?: number;
  dayOfWeek: string;
  startTime: string; // "09:00:00"
  endTime: string;   // "17:00:00"
  isActive?: boolean;
}

export interface DoctorAvailabilityRequest {
  clinicId?: number;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  isActive?: boolean;
}

export interface SlotInfo {
  startTime: string;
  endTime: string;
  displayTime: string;
}

export interface TimeSlotResponse {
  doctorId: number;
  doctorName: string;
  date: string;
  availableSlots: SlotInfo[];
}
