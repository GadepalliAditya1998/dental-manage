import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  PatientLookupRequest,
  PatientLookupResponse,
  PatientRequest,
  PatientResponse,
  PatientUpcomingAppointmentResponse
} from '../models/patient.model';

@Injectable({
  providedIn: 'root'
})
export class PatientService {
  private readonly baseUrl = `${environment.apiUrl}/patients`;

  constructor(private http: HttpClient) {}

  lookupPatient(request: PatientLookupRequest): Observable<ApiResponse<PatientLookupResponse>> {
    let params = new HttpParams();
    if (request.patientId) params = params.set('patientId', request.patientId);
    if (request.phoneNumber) params = params.set('phoneNumber', request.phoneNumber);
    if (request.mobileNumber) params = params.set('mobileNumber', request.mobileNumber);
    if (request.fullName) params = params.set('fullName', request.fullName);
    if (request.dateOfBirth) params = params.set('dateOfBirth', request.dateOfBirth);
    if (request.clinicId) params = params.set('clinicId', request.clinicId.toString());

    return this.http.get<ApiResponse<PatientLookupResponse>>(`${this.baseUrl}/lookup`, { params });
  }

  getPatients(search?: string): Observable<ApiResponse<PatientResponse[]>> {
    let params = new HttpParams();
    if (search) params = params.set('search', search);
    return this.http.get<ApiResponse<PatientResponse[]>>(this.baseUrl, { params });
  }

  getPatientById(id: number): Observable<ApiResponse<PatientResponse>> {
    return this.http.get<ApiResponse<PatientResponse>>(`${this.baseUrl}/${id}`);
  }

  getUpcomingAppointments(patientId: number): Observable<ApiResponse<PatientUpcomingAppointmentResponse[]>> {
    return this.http.get<ApiResponse<PatientUpcomingAppointmentResponse[]>>(`${this.baseUrl}/${patientId}/appointments/upcoming`);
  }

  createPatient(request: PatientRequest): Observable<ApiResponse<PatientResponse>> {
    return this.http.post<ApiResponse<PatientResponse>>(this.baseUrl, request);
  }

  updatePatient(id: number, request: PatientRequest): Observable<ApiResponse<PatientResponse>> {
    return this.http.put<ApiResponse<PatientResponse>>(`${this.baseUrl}/${id}`, request);
  }

  deletePatient(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }
}
