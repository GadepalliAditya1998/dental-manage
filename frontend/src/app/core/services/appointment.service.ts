import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  AppointmentRequest,
  AppointmentResponse,
  AppointmentStatus,
  BookingSource,
  CalendarEventRequest
} from '../models/appointment.model';
import { TimeSlotResponse } from '../models/doctor.model';

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private readonly baseUrl = `${environment.apiUrl}/appointments`;

  constructor(private http: HttpClient) {}

  getAppointments(filters?: {
    date?: string;
    doctorId?: number;
    patientId?: number;
    status?: AppointmentStatus;
    bookingSource?: BookingSource;
  }): Observable<ApiResponse<AppointmentResponse[]>> {
    let params = new HttpParams();
    if (filters?.date) params = params.set('date', filters.date);
    if (filters?.doctorId) params = params.set('doctorId', filters.doctorId.toString());
    if (filters?.patientId) params = params.set('patientId', filters.patientId.toString());
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.bookingSource) params = params.set('bookingSource', filters.bookingSource);

    return this.http.get<ApiResponse<AppointmentResponse[]>>(this.baseUrl, { params });
  }

  getAppointmentById(id: number): Observable<ApiResponse<AppointmentResponse>> {
    return this.http.get<ApiResponse<AppointmentResponse>>(`${this.baseUrl}/${id}`);
  }

  createAppointment(request: AppointmentRequest): Observable<ApiResponse<AppointmentResponse>> {
    return this.http.post<ApiResponse<AppointmentResponse>>(this.baseUrl, request);
  }

  updateAppointment(id: number, request: AppointmentRequest): Observable<ApiResponse<AppointmentResponse>> {
    return this.http.put<ApiResponse<AppointmentResponse>>(`${this.baseUrl}/${id}`, request);
  }

  updateStatus(id: number, status: AppointmentStatus): Observable<ApiResponse<AppointmentResponse>> {
    return this.http.patch<ApiResponse<AppointmentResponse>>(`${this.baseUrl}/${id}/status`, null, {
      params: { status }
    });
  }

  cancelAppointment(id: number, reason?: string): Observable<ApiResponse<AppointmentResponse>> {
    let params = new HttpParams();
    if (reason) params = params.set('reason', reason);
    return this.http.patch<ApiResponse<AppointmentResponse>>(`${this.baseUrl}/${id}/cancel`, null, { params });
  }

  getAvailableSlots(doctorId: number, date: string): Observable<ApiResponse<TimeSlotResponse>> {
    const params = new HttpParams().set('doctorId', doctorId.toString()).set('date', date);
    return this.http.get<ApiResponse<TimeSlotResponse>>(`${this.baseUrl}/slots`, { params });
  }

  addCalendarEvent(request: CalendarEventRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/${request.appointmentId}/calendar-event`, request);
  }
}
