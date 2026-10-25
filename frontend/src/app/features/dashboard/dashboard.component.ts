import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
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
  metricsLoading = false;
  errorMessage = '';

  selectedCall: CallRecordResponse | null = null;

  // Filter state: Day, Week, Month, Custom
  selectedFilter: 'DAY' | 'WEEK' | 'MONTH' | 'CUSTOM' = 'DAY';
  customStartDate = '';
  customEndDate = '';
  todayDate = new Date().toISOString().split('T')[0];
  dateRangeLabel = 'Today';

  private wsSubs = new Subscription();

  constructor(
    private dashboardService: DashboardService,
    private appointmentService: AppointmentService,
    private callRecordService: CallRecordService,
    private wsService: WebSocketService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  get isDoctor(): boolean {
    return this.contextUser?.role?.toUpperCase() === 'DOCTOR';
  }

  ngOnInit(): void {
    this.authService.contextUser$.subscribe((user) => {
      this.contextUser = user;
    });
    this.contextUser = this.authService.getContextUser();

    // Default custom range to past 7 days
    const pastDate = new Date();
    pastDate.setDate(pastDate.getDate() - 7);
    this.customStartDate = pastDate.toISOString().split('T')[0];
    this.customEndDate = this.todayDate;

    this.updateDateRangeLabel();
    this.loadDashboardData();
    this.setupRealtimeListeners();
  }

  setFilter(filter: 'DAY' | 'WEEK' | 'MONTH' | 'CUSTOM'): void {
    if (this.selectedFilter === filter) return;
    this.selectedFilter = filter;
    this.updateDateRangeLabel();
    this.loadMetrics();
  }

  applyCustomFilter(): void {
    if (this.customStartDate && this.customEndDate) {
      if (this.customStartDate > this.customEndDate) {
        const temp = this.customStartDate;
        this.customStartDate = this.customEndDate;
        this.customEndDate = temp;
      }
      this.updateDateRangeLabel();
      this.loadMetrics();
    }
  }

  updateDateRangeLabel(): void {
    const today = new Date();
    const options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric', year: 'numeric' };

    switch (this.selectedFilter) {
      case 'DAY':
        this.dateRangeLabel = `Today, ${today.toLocaleDateString('en-US', options)}`;
        break;
      case 'WEEK': {
        const day = today.getDay();
        const diffToMonday = today.getDate() - day + (day === 0 ? -6 : 1);
        const monday = new Date(today.setDate(diffToMonday));
        const sunday = new Date(monday);
        sunday.setDate(monday.getDate() + 6);
        this.dateRangeLabel = `This Week (${monday.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })} - ${sunday.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })})`;
        break;
      }
      case 'MONTH':
        this.dateRangeLabel = `This Month (${new Date().toLocaleDateString('en-US', { month: 'long', year: 'numeric' })})`;
        break;
      case 'CUSTOM':
        if (this.customStartDate && this.customEndDate) {
          this.dateRangeLabel = `${this.customStartDate} to ${this.customEndDate}`;
        } else {
          this.dateRangeLabel = 'Custom Range';
        }
        break;
    }
  }

  loadDashboardData(): void {
    this.loading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.loadMetrics(() => {
      this.loading = false;
      this.cdr.markForCheck();
    });

    this.loadAppointments();
    this.loadRecentCalls();
  }

  loadMetrics(onComplete?: () => void): void {
    this.metricsLoading = true;
    this.cdr.markForCheck();
    const doctorId = this.isDoctor && this.contextUser?.doctorId ? this.contextUser.doctorId : undefined;
    const startDate = this.selectedFilter === 'CUSTOM' ? this.customStartDate : undefined;
    const endDate = this.selectedFilter === 'CUSTOM' ? this.customEndDate : undefined;

    this.dashboardService.getMetrics(this.selectedFilter, startDate, endDate, doctorId).subscribe({
      next: (res) => {
        if (res?.data) {
          this.metrics = res.data;
          this.errorMessage = '';
        }
        this.metricsLoading = false;
        this.cdr.markForCheck();
        if (onComplete) onComplete();
      },
      error: (err) => {
        console.warn('Dashboard metrics API unreachable:', err);
        this.errorMessage = 'Backend API is currently pending or unreachable. Waiting for backend connection.';
        this.metricsLoading = false;
        this.cdr.markForCheck();
        if (onComplete) onComplete();
      }
    });
  }

  loadAppointments(): void {
    const today = new Date().toISOString().split('T')[0];
    const apptFilters: any = { date: today };
    if (this.isDoctor && this.contextUser?.doctorId) {
      apptFilters.doctorId = this.contextUser.doctorId;
    }

    this.appointmentService.getAppointments(apptFilters).subscribe({
      next: (res) => {
        if (res?.data) {
          this.recentAppointments = res.data.slice(0, 10);
          this.cdr.markForCheck();
        }
      },
      error: (err) => {
        console.warn('Appointments API unreachable:', err);
        this.cdr.markForCheck();
      }
    });
  }

  loadRecentCalls(): void {
    const doctorId = this.isDoctor && this.contextUser?.doctorId ? this.contextUser.doctorId : undefined;

    this.callRecordService.getRecentCalls(5, doctorId).subscribe({
      next: (res) => {
        if (res?.data) {
          this.recentCalls = res.data;
          this.cdr.markForCheck();
        }
      },
      error: (err) => {
        console.warn('Calls API unreachable:', err);
        this.cdr.markForCheck();
      }
    });
  }

  setupRealtimeListeners(): void {
    this.wsSubs.add(
      this.wsService.onCallUpdate().subscribe((call) => {
        if (call) {
          this.recentCalls.unshift(call);
          if (this.recentCalls.length > 5) this.recentCalls.pop();
          if (this.metrics) {
            this.metrics.callsReceived = (this.metrics.callsReceived || 0) + 1;
            this.metrics.totalAiCalls = (this.metrics.totalAiCalls || 0) + 1;
          }
        }
      })
    );

    this.wsSubs.add(
      this.wsService.onAppointmentUpdate().subscribe((appt) => {
        if (appt) {
          this.recentAppointments.unshift(appt);
          if (this.recentAppointments.length > 10) this.recentAppointments.pop();
          if (this.metrics) {
            this.metrics.successfulBookings = (this.metrics.successfulBookings || 0) + 1;
            if (appt.bookingSource === 'AI_AGENT' || (appt as any).callRecord) {
              this.metrics.appointmentsBookedViaCallAgent = (this.metrics.appointmentsBookedViaCallAgent || 0) + 1;
            }
          }
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
