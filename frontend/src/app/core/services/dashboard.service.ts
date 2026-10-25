import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { DashboardMetricsResponse } from '../models/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly baseUrl = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  getMetrics(
    filter?: string,
    startDate?: string,
    endDate?: string,
    doctorId?: number
  ): Observable<ApiResponse<DashboardMetricsResponse>> {
    let params = new HttpParams();
    if (filter) params = params.set('filter', filter);
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    if (doctorId !== undefined && doctorId !== null) params = params.set('doctorId', doctorId.toString());

    return this.http.get<ApiResponse<DashboardMetricsResponse>>(`${this.baseUrl}/metrics`, { params });
  }

  getDailyMetrics(
    startDate?: string,
    endDate?: string,
    doctorId?: number
  ): Observable<ApiResponse<DashboardMetricsResponse>> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    if (doctorId !== undefined && doctorId !== null) params = params.set('doctorId', doctorId.toString());

    return this.http.get<ApiResponse<DashboardMetricsResponse>>(`${this.baseUrl}/daily`, { params });
  }
}
