import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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

  getMetrics(): Observable<ApiResponse<DashboardMetricsResponse>> {
    return this.http.get<ApiResponse<DashboardMetricsResponse>>(`${this.baseUrl}/metrics`);
  }

  getDailyMetrics(startDate?: string, endDate?: string): Observable<ApiResponse<DashboardMetricsResponse>> {
    const params: { [key: string]: string } = {};
    if (startDate) params['startDate'] = startDate;
    if (endDate) params['endDate'] = endDate;
    return this.http.get<ApiResponse<DashboardMetricsResponse>>(`${this.baseUrl}/daily`, { params });
  }
}
