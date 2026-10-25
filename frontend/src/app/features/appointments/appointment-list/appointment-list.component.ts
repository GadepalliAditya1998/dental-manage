import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { AppointmentService } from '../../../core/services/appointment.service';
import { DoctorService } from '../../../core/services/doctor.service';
import { AppointmentResponse, AppointmentStatus } from '../../../core/models/appointment.model';
import { DoctorResponse } from '../../../core/models/doctor.model';

@Component({
  selector: 'app-appointment-list',
  standalone: false,
  templateUrl: './appointment-list.component.html',
  styleUrls: ['./appointment-list.component.scss']
})
export class AppointmentListComponent implements OnInit {
  appointments: AppointmentResponse[] = [];
  doctors: DoctorResponse[] = [];
  loading = false;
  errorMessage = '';

  filterDate = '';
  filterDoctorId: number | null = null;
  filterStatus: AppointmentStatus | '' = '';

  // Cancel dialog
  cancellingAppt: AppointmentResponse | null = null;
  cancelReason = '';

  constructor(
    private appointmentService: AppointmentService,
    private doctorService: DoctorService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Default to today
    this.filterDate = new Date().toISOString().split('T')[0];
    this.loadDoctors();
    this.loadAppointments();
  }

  loadDoctors(): void {
    this.doctorService.getDoctors().subscribe({
      next: (res) => {
        const list = Array.isArray(res) ? res : (res?.data || []);
        this.doctors = Array.isArray(list) ? list : [];
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.warn('Could not load doctors', err);
        this.cdr.markForCheck();
      }
    });
  }

  loadAppointments(): void {
    this.loading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    const filters: any = {};
    if (this.filterDate) filters.date = this.filterDate;
    if (this.filterDoctorId) filters.doctorId = this.filterDoctorId;
    if (this.filterStatus) filters.status = this.filterStatus;

    this.appointmentService.getAppointments(filters).subscribe({
      next: (res) => {
        const list = Array.isArray(res) ? res : (res?.data || []);
        this.appointments = Array.isArray(list) ? list : [];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.warn('Could not load appointments', err);
        this.errorMessage = 'Backend endpoint GET /api/appointments is pending or unreachable.';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  onFilterChange(): void {
    this.loadAppointments();
  }

  updateStatus(appt: AppointmentResponse, newStatus: AppointmentStatus): void {
    this.appointmentService.updateStatus(appt.id, newStatus).subscribe({
      next: () => {
        appt.status = newStatus;
        this.cdr.markForCheck();
      },
      error: (err) => {
        alert('Failed to update status. Backend API may be unreachable.');
        this.cdr.markForCheck();
      }
    });
  }

  openCancelModal(appt: AppointmentResponse): void {
    this.cancellingAppt = appt;
    this.cancelReason = '';
    this.cdr.markForCheck();
  }

  closeCancelModal(): void {
    this.cancellingAppt = null;
    this.cancelReason = '';
    this.cdr.markForCheck();
  }

  confirmCancel(): void {
    if (!this.cancellingAppt) return;
    this.appointmentService.cancelAppointment(this.cancellingAppt.id, this.cancelReason).subscribe({
      next: () => {
        if (this.cancellingAppt) {
          this.cancellingAppt.status = 'CANCELLED';
        }
        this.closeCancelModal();
        this.cdr.markForCheck();
      },
      error: (err) => {
        alert('Failed to cancel appointment. Backend API may be unreachable.');
        this.cdr.markForCheck();
      }
    });
  }

  addCalendarEvent(appt: AppointmentResponse): void {
    this.appointmentService.addCalendarEvent({
      appointmentId: appt.id,
      patientEmail: appt.patient.email
    }).subscribe({
      next: () => {
        appt.calendarEventAdded = true;
        alert('Calendar invite queued successfully.');
        this.cdr.markForCheck();
      },
      error: (err) => {
        alert('Failed to queue calendar invite. Backend API may be unreachable.');
        this.cdr.markForCheck();
      }
    });
  }
}
