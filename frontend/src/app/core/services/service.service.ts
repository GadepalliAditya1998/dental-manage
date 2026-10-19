import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { ServiceRequest, ServiceResponse } from '../models/service.model';

@Injectable({
  providedIn: 'root'
})
export class ServiceCatalogService {
  private readonly baseUrl = `${environment.apiUrl}/services`;

  constructor(private http: HttpClient) {}

  getServices(): Observable<ApiResponse<ServiceResponse[]>> {
    return this.http.get<ApiResponse<ServiceResponse[]>>(this.baseUrl);
  }

  getServiceById(id: number): Observable<ApiResponse<ServiceResponse>> {
    return this.http.get<ApiResponse<ServiceResponse>>(`${this.baseUrl}/${id}`);
  }

  createService(request: ServiceRequest): Observable<ApiResponse<ServiceResponse>> {
    return this.http.post<ApiResponse<ServiceResponse>>(this.baseUrl, request);
  }

  updateService(id: number, request: ServiceRequest): Observable<ApiResponse<ServiceResponse>> {
    return this.http.put<ApiResponse<ServiceResponse>>(`${this.baseUrl}/${id}`, request);
  }

  deleteService(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }
}
