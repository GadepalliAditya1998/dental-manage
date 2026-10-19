export interface PatientResponse {
  id: number;
  clinicId?: number;
  firstName: string;
  lastName?: string;
  email?: string;
  phoneNumber: string;
  isNewLead?: boolean;
  createdOn?: string;
  modifiedOn?: string;
}

export interface PatientRequest {
  clinicId?: number;
  firstName: string;
  lastName?: string;
  email?: string;
  phoneNumber: string;
}
