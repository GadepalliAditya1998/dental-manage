export interface ServiceResponse {
  id: number;
  clinicId?: number;
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
  isActive?: boolean;
}

export interface ServiceRequest {
  clinicId?: number;
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
  isActive?: boolean;
}
