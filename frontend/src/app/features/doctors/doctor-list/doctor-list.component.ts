import { Component, OnInit } from '@angular/core';
import { DoctorService } from '../../../core/services/doctor.service';
import { DoctorResponse } from '../../../core/models/doctor.model';

@Component({
  selector: 'app-doctor-list',
  standalone: false,
  templateUrl: './doctor-list.component.html',
  styleUrls: ['./doctor-list.component.scss']
})
export class DoctorListComponent implements OnInit {
  doctors: DoctorResponse[] = [];
  loading = false;
  errorMessage = '';

  constructor(private doctorService: DoctorService) {}

  ngOnInit(): void {
    this.loadDoctors();
  }

  loadDoctors(): void {
    this.loading = true;
    this.errorMessage = '';

    this.doctorService.getDoctors().subscribe({
      next: (res) => {
        this.doctors = res?.data || [];
        this.loading = false;
      },
      error: (err) => {
        console.warn('Doctors API error:', err);
        this.errorMessage = 'Backend endpoint GET /api/doctors is pending or unreachable.';
        this.loading = false;
      }
    });
  }

  toggleActive(doc: DoctorResponse): void {
    const updated = {
      firstName: doc.firstName,
      lastName: doc.lastName,
      specialization: doc.specialization,
      email: doc.email,
      phoneNumber: doc.phoneNumber,
      isActive: !doc.isActive
    };

    this.doctorService.updateDoctor(doc.id, updated).subscribe({
      next: () => {
        doc.isActive = !doc.isActive;
      },
      error: () => alert('Failed to update doctor status. Backend API may be unreachable.')
    });
  }

  deleteDoctor(id: number): void {
    if (!confirm('Are you sure you want to remove this doctor from the clinic?')) return;
    this.doctorService.deleteDoctor(id).subscribe({
      next: () => {
        this.doctors = this.doctors.filter(d => d.id !== id);
      },
      error: () => alert('Failed to delete doctor. Backend API may be unreachable.')
    });
  }
}
