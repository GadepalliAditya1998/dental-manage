import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { PatientRequest, PatientResponse } from '../models/patient.model';

@Injectable({
  providedIn: 'root'
})
export class PatientService {
  private readonly baseUrl = `${environment.apiUrl}/patients`;

  constructor(private http: HttpClient) {}

  getPatients(search?: string): Observable<ApiResponse<PatientResponse[]>> {
    let params = new HttpParams();
    if (search) params = params.set('search', search);
    return this.http.get<ApiResponse<PatientResponse[]>>(this.baseUrl, { params });
  }

  getPatientById(id: number): Observable<ApiResponse<PatientResponse>> {
    return this.http.get<ApiResponse<PatientResponse>>(`${this.baseUrl}/${id}`);
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
