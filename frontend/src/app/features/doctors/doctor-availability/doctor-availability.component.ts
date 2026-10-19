import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DoctorService } from '../../../core/services/doctor.service';
import { DoctorAvailabilityRequest, DoctorAvailabilityResponse, DoctorResponse } from '../../../core/models/doctor.model';

interface DaySchedule {
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  isActive: boolean;
}

@Component({
  selector: 'app-doctor-availability',
  standalone: false,
  templateUrl: './doctor-availability.component.html',
  styleUrls: ['./doctor-availability.component.scss']
})
export class DoctorAvailabilityComponent implements OnInit {
  doctorId!: number;
  doctor: DoctorResponse | null = null;
  loading = false;
  saving = false;
  successMsg = '';
  errorMsg = '';

  days: DaySchedule[] = [
    { dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '17:00', isActive: true },
    { dayOfWeek: 'TUESDAY', startTime: '09:00', endTime: '17:00', isActive: true },
    { dayOfWeek: 'WEDNESDAY', startTime: '09:00', endTime: '17:00', isActive: true },
    { dayOfWeek: 'THURSDAY', startTime: '09:00', endTime: '17:00', isActive: true },
    { dayOfWeek: 'FRIDAY', startTime: '09:00', endTime: '17:00', isActive: true },
    { dayOfWeek: 'SATURDAY', startTime: '10:00', endTime: '14:00', isActive: true },
    { dayOfWeek: 'SUNDAY', startTime: '10:00', endTime: '14:00', isActive: false }
  ];

  constructor(
    private route: ActivatedRoute,
    private doctorService: DoctorService
  ) {}

  ngOnInit(): void {
    this.doctorId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadDoctorInfo();
    this.loadAvailability();
  }

  loadDoctorInfo(): void {
    this.doctorService.getDoctorById(this.doctorId).subscribe({
      next: (res) => this.doctor = res?.data || null,
      error: (err) => console.warn('Could not load doctor details', err)
    });
  }

  loadAvailability(): void {
    this.loading = true;
    this.doctorService.getDoctorAvailability(this.doctorId).subscribe({
      next: (res) => {
        if (res?.data && res.data.length > 0) {
          // Merge existing availability into days
          res.data.forEach((item: DoctorAvailabilityResponse) => {
            const found = this.days.find(d => d.dayOfWeek.toUpperCase() === item.dayOfWeek.toUpperCase());
            if (found) {
              found.startTime = item.startTime?.substring(0, 5) || found.startTime;
              found.endTime = item.endTime?.substring(0, 5) || found.endTime;
              found.isActive = item.isActive ?? true;
            }
          });
        }
        this.loading = false;
      },
      error: (err) => {
        console.warn('Availability API unreachable', err);
        this.loading = false;
      }
    });
  }

  saveAvailability(): void {
    this.saving = true;
    this.errorMsg = '';
    this.successMsg = '';

    const payload: DoctorAvailabilityRequest[] = this.days.map(d => ({
      dayOfWeek: d.dayOfWeek,
      startTime: d.startTime.length === 5 ? `${d.startTime}:00` : d.startTime,
      endTime: d.endTime.length === 5 ? `${d.endTime}:00` : d.endTime,
      isActive: d.isActive
    }));

    this.doctorService.updateDoctorAvailability(this.doctorId, payload).subscribe({
      next: () => {
        this.saving = false;
        this.successMsg = 'Doctor availability schedule saved successfully!';
      },
      error: (err) => {
        this.saving = false;
        this.errorMsg = 'Failed to save availability: Backend PUT /api/doctors/{id}/availability is pending.';
      }
    });
  }
}
