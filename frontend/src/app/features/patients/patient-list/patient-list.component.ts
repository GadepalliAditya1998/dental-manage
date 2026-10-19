import { Component, OnInit } from '@angular/core';
import { PatientService } from '../../../core/services/patient.service';
import { PatientResponse } from '../../../core/models/patient.model';

@Component({
  selector: 'app-patient-list',
  standalone: false,
  templateUrl: './patient-list.component.html',
  styleUrls: ['./patient-list.component.scss']
})
export class PatientListComponent implements OnInit {
  patients: PatientResponse[] = [];
  filteredPatients: PatientResponse[] = [];
  loading = false;
  searchQuery = '';
  leadFilter: 'ALL' | 'NEW' | 'EXISTING' = 'ALL';
  errorMessage = '';

  constructor(private patientService: PatientService) {}

  ngOnInit(): void {
    this.loadPatients();
  }

  loadPatients(): void {
    this.loading = true;
    this.errorMessage = '';

    this.patientService.getPatients().subscribe({
      next: (res) => {
        this.patients = res?.data || [];
        this.applyFilter();
        this.loading = false;
      },
      error: (err) => {
        console.warn('Patients API error:', err);
        this.errorMessage = 'Backend endpoint GET /api/patients is pending or unreachable.';
        this.loading = false;
      }
    });
  }

  applyFilter(): void {
    let result = this.patients;

    if (this.leadFilter === 'NEW') {
      result = result.filter(p => p.isNewLead);
    } else if (this.leadFilter === 'EXISTING') {
      result = result.filter(p => !p.isNewLead);
    }

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(p =>
        (p.firstName?.toLowerCase().includes(q) ||
        p.lastName?.toLowerCase().includes(q) ||
        p.phoneNumber?.includes(q) ||
        p.email?.toLowerCase().includes(q))
      );
    }

    this.filteredPatients = result;
  }

  deletePatient(id: number): void {
    if (!confirm('Are you sure you want to delete this patient record?')) return;
    this.patientService.deletePatient(id).subscribe({
      next: () => {
        this.patients = this.patients.filter(p => p.id !== id);
        this.applyFilter();
      },
      error: (err) => alert('Failed to delete patient. Backend API may be unreachable.')
    });
  }
}
