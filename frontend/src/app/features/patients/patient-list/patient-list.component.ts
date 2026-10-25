import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
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

  constructor(
    private patientService: PatientService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadPatients();
  }

  loadPatients(): void {
    this.loading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.patientService.getPatients().subscribe({
      next: (res) => {
        const patientList = Array.isArray(res) ? res : (res?.data || []);
        this.patients = Array.isArray(patientList) ? patientList : [];
        this.applyFilter();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.warn('Patients API error:', err);
        this.errorMessage = 'Backend endpoint GET /api/patients is pending or unreachable.';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  applyFilter(): void {
    let result = Array.isArray(this.patients) ? [...this.patients] : [];

    if (this.leadFilter === 'NEW') {
      result = result.filter(p => p.isNewLead);
    } else if (this.leadFilter === 'EXISTING') {
      result = result.filter(p => !p.isNewLead);
    }

    if (this.searchQuery && this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase().trim();
      result = result.filter(p =>
        (p.firstName?.toLowerCase().includes(q) ||
        p.lastName?.toLowerCase().includes(q) ||
        p.fullName?.toLowerCase().includes(q) ||
        p.patientId?.toLowerCase().includes(q) ||
        p.phoneNumber?.includes(q) ||
        p.email?.toLowerCase().includes(q))
      );
    }

    this.filteredPatients = result;
    this.cdr.markForCheck();
  }

  deletePatient(id: number): void {
    if (!confirm('Are you sure you want to delete this patient record?')) return;
    this.patientService.deletePatient(id).subscribe({
      next: () => {
        this.patients = this.patients.filter(p => p.id !== id);
        this.applyFilter();
        this.cdr.markForCheck();
      },
      error: () => {
        alert('Failed to delete patient. Backend API may be unreachable.');
        this.cdr.markForCheck();
      }
    });
  }
}
