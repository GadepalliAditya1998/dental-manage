import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { DashboardService } from '../../core/services/dashboard.service';
import { AppointmentService } from '../../core/services/appointment.service';
import { CallRecordService } from '../../core/services/call-record.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { AuthService } from '../../core/services/auth.service';
import { DashboardMetricsResponse } from '../../core/models/dashboard.model';
import { AppointmentResponse } from '../../core/models/appointment.model';
import { CallRecordResponse } from '../../core/models/call-record.model';
import { ContextUser } from '../../core/models/auth.model';

@Component({
  selector: 'app-dashboard',
  standalone: false,
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  contextUser: ContextUser | null = null;
  metrics: DashboardMetricsResponse | null = null;
  recentAppointments: AppointmentResponse[] = [];
  recentCalls: CallRecordResponse[] = [];
  loading = true;
  errorMessage = '';

  selectedCall: CallRecordResponse | null = null;

  private wsSubs = new Subscription();

  constructor(
    private dashboardService: DashboardService,
    private appointmentService: AppointmentService,
    private callRecordService: CallRecordService,
    private wsService: WebSocketService,
    private authService: AuthService
  ) {}

  get isDoctor(): boolean {
    return this.contextUser?.role?.toUpperCase() === 'DOCTOR';
  }

  ngOnInit(): void {
    this.authService.contextUser$.subscribe((user) => {
      this.contextUser = user;
    });
    this.contextUser = this.authService.getContextUser();

    this.loadDashboardData();
    this.setupRealtimeListeners();
  }

  loadDashboardData(): void {
    this.loading = true;
    this.errorMessage = '';

    // Load Metrics
    this.dashboardService.getMetrics().subscribe({
      next: (res) => {
        if (res?.data) {
          this.metrics = res.data;
        }
        this.loading = false;
      },
      error: (err) => {
        console.warn('Dashboard metrics API unreachable:', err);
        this.errorMessage = 'Backend API is currently pending or unreachable. Waiting for backend connection.';
        this.loading = false;
      }
    });

    // Load Today's Appointments (scoped to doctor if doctor is logged in)
    const today = new Date().toISOString().split('T')[0];
    const apptFilters: any = { date: today };
    if (this.isDoctor && this.contextUser?.doctorId) {
      apptFilters.doctorId = this.contextUser.doctorId;
    }

    this.appointmentService.getAppointments(apptFilters).subscribe({
      next: (res) => {
        if (res?.data) {
          this.recentAppointments = res.data.slice(0, 10);
        }
      },
      error: (err) => console.warn('Appointments API unreachable:', err)
    });

    // Load Recent Calls (only relevant for receptionist / general overview)
    if (!this.isDoctor) {
      this.callRecordService.getRecentCalls(5).subscribe({
        next: (res) => {
          if (res?.data) {
            this.recentCalls = res.data;
          }
        },
        error: (err) => console.warn('Calls API unreachable:', err)
      });
    }
  }

  setupRealtimeListeners(): void {
    this.wsSubs.add(
      this.wsService.onCallUpdate().subscribe((call) => {
        if (call && !this.isDoctor) {
          this.recentCalls.unshift(call);
          if (this.recentCalls.length > 5) this.recentCalls.pop();
          if (this.metrics) {
            this.metrics.totalAiCalls++;
          }
        }
      })
    );

    this.wsSubs.add(
      this.wsService.onAppointmentUpdate().subscribe((appt) => {
        if (appt) {
          this.recentAppointments.unshift(appt);
          if (this.recentAppointments.length > 10) this.recentAppointments.pop();
        }
      })
    );
  }

  viewCallDetails(call: CallRecordResponse): void {
    this.selectedCall = call;
  }

  closeModal(): void {
    this.selectedCall = null;
  }

  ngOnDestroy(): void {
    this.wsSubs.unsubscribe();
  }
}
