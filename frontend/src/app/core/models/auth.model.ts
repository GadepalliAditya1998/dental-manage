export interface LoginRequest {
  username: string; // Base64 encoded before sending
  password: string; // Base64 encoded before sending
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn?: number;
  id?: number;
  username: string;
  role: 'DOCTOR' | 'RECEPTIONIST' | 'PATIENT' | string;
  fullName: string;
  email?: string;
  phoneNumber?: string;
  clinicId?: number;
  doctorId?: number;
}

export interface TokenValidationResponse {
  valid: boolean;
  username: string;
  role: string;
  fullName?: string;
  email?: string;
  phoneNumber?: string;
  doctorId?: number;
  clinicId?: number;
}

/**
 * Frontend ContextUser object.
 * Excludes the password field and holds authenticated user metadata.
 */
export interface ContextUser {
  name: string;
  role: 'DOCTOR' | 'RECEPTIONIST' | string;
  email?: string;
  phoneNumber?: string;
  token: string;
  username?: string;
  clinicId?: number;
  doctorId?: number;
}
