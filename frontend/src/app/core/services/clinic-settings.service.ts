import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { ClinicSettingsRequest, ClinicSettingsResponse } from '../models/clinic-settings.model';

@Injectable({
  providedIn: 'root'
})
export class ClinicSettingsService {
  private readonly baseUrl = `${environment.apiUrl}/settings`;

  constructor(private http: HttpClient) {}

  getSettings(): Observable<ApiResponse<ClinicSettingsResponse>> {
    return this.http.get<ApiResponse<ClinicSettingsResponse>>(this.baseUrl);
  }

  updateSettings(request: ClinicSettingsRequest): Observable<ApiResponse<ClinicSettingsResponse>> {
    return this.http.put<ApiResponse<ClinicSettingsResponse>>(this.baseUrl, request);
  }
}
