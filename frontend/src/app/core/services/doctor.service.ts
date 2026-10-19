import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  DoctorAvailabilityRequest,
  DoctorAvailabilityResponse,
  DoctorRequest,
  DoctorResponse
} from '../models/doctor.model';

@Injectable({
  providedIn: 'root'
})
export class DoctorService {
  private readonly baseUrl = `${environment.apiUrl}/doctors`;

  constructor(private http: HttpClient) {}

  getDoctors(): Observable<ApiResponse<DoctorResponse[]>> {
    return this.http.get<ApiResponse<DoctorResponse[]>>(this.baseUrl);
  }

  getDoctorById(id: number): Observable<ApiResponse<DoctorResponse>> {
    return this.http.get<ApiResponse<DoctorResponse>>(`${this.baseUrl}/${id}`);
  }

  createDoctor(request: DoctorRequest): Observable<ApiResponse<DoctorResponse>> {
    return this.http.post<ApiResponse<DoctorResponse>>(this.baseUrl, request);
  }

  updateDoctor(id: number, request: DoctorRequest): Observable<ApiResponse<DoctorResponse>> {
    return this.http.put<ApiResponse<DoctorResponse>>(`${this.baseUrl}/${id}`, request);
  }

  deleteDoctor(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }

  getDoctorAvailability(doctorId: number): Observable<ApiResponse<DoctorAvailabilityResponse[]>> {
    return this.http.get<ApiResponse<DoctorAvailabilityResponse[]>>(`${this.baseUrl}/${doctorId}/availability`);
  }

  updateDoctorAvailability(
    doctorId: number,
    availabilities: DoctorAvailabilityRequest[]
  ): Observable<ApiResponse<DoctorAvailabilityResponse[]>> {
    return this.http.put<ApiResponse<DoctorAvailabilityResponse[]>>(
      `${this.baseUrl}/${doctorId}/availability`,
      availabilities
    );
  }
}
