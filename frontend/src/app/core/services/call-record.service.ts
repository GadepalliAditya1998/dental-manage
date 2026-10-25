import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { CallRecordResponse } from '../models/call-record.model';

@Injectable({
  providedIn: 'root'
})
export class CallRecordService {
  private readonly baseUrl = `${environment.apiUrl}/calls`;

  constructor(private http: HttpClient) {}

  getCalls(filters?: {
    callType?: string;
    page?: number;
    size?: number;
  }): Observable<ApiResponse<CallRecordResponse[]>> {
    let params = new HttpParams();
    if (filters?.callType) params = params.set('callType', filters.callType);
    if (filters?.page !== undefined) params = params.set('page', filters.page.toString());
    if (filters?.size !== undefined) params = params.set('size', filters.size.toString());

    return this.http.get<ApiResponse<CallRecordResponse[]>>(this.baseUrl, { params });
  }

  getRecentCalls(limit: number = 10, doctorId?: number): Observable<ApiResponse<CallRecordResponse[]>> {
    let params = new HttpParams().set('limit', limit.toString());
    if (doctorId !== undefined && doctorId !== null) {
      params = params.set('doctorId', doctorId.toString());
    }
    return this.http.get<ApiResponse<CallRecordResponse[]>>(`${this.baseUrl}/recent`, { params });
  }

  getCallById(id: number): Observable<ApiResponse<CallRecordResponse>> {
    return this.http.get<ApiResponse<CallRecordResponse>>(`${this.baseUrl}/${id}`);
  }
}
