import { Component, OnInit } from '@angular/core';
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
    private doctorService: DoctorService
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
        if (res?.data) this.doctors = res.data;
      },
      error: (err) => console.warn('Could not load doctors', err)
    });
  }

  loadAppointments(): void {
    this.loading = true;
    this.errorMessage = '';

    const filters: any = {};
    if (this.filterDate) filters.date = this.filterDate;
    if (this.filterDoctorId) filters.doctorId = this.filterDoctorId;
    if (this.filterStatus) filters.status = this.filterStatus;

    this.appointmentService.getAppointments(filters).subscribe({
      next: (res) => {
        this.appointments = res?.data || [];
        this.loading = false;
      },
      error: (err) => {
        console.warn('Could not load appointments', err);
        this.errorMessage = 'Backend endpoint GET /api/appointments is pending or unreachable.';
        this.loading = false;
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
      },
      error: (err) => alert('Failed to update status. Backend API may be unreachable.')
    });
  }

  openCancelModal(appt: AppointmentResponse): void {
    this.cancellingAppt = appt;
    this.cancelReason = '';
  }

  closeCancelModal(): void {
    this.cancellingAppt = null;
    this.cancelReason = '';
  }

  confirmCancel(): void {
    if (!this.cancellingAppt) return;
    this.appointmentService.cancelAppointment(this.cancellingAppt.id, this.cancelReason).subscribe({
      next: () => {
        if (this.cancellingAppt) {
          this.cancellingAppt.status = 'CANCELLED';
        }
        this.closeCancelModal();
      },
      error: (err) => alert('Failed to cancel appointment. Backend API may be unreachable.')
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
      },
      error: (err) => alert('Failed to queue calendar invite. Backend API may be unreachable.')
    });
  }
}
