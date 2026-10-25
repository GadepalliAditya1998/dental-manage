import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PatientService } from '../../../core/services/patient.service';

@Component({
  selector: 'app-patient-form',
  standalone: false,
  templateUrl: './patient-form.component.html',
  styleUrls: ['./patient-form.component.scss']
})
export class PatientFormComponent implements OnInit {
  patientForm!: FormGroup;
  isEditMode = false;
  patientId: number | null = null;
  submitting = false;
  errorMsg = '';
  successMsg = '';

  constructor(
    private fb: FormBuilder,
    private patientService: PatientService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initForm();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.patientId = Number(idParam);
      this.loadPatientData(this.patientId);
    }
  }

  initForm(): void {
    this.patientForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: [''],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^[+]?[0-9\s-]{7,15}$/)]],
      dateOfBirth: ['', [Validators.required]]
    });
  }

  loadPatientData(id: number): void {
    this.patientService.getPatientById(id).subscribe({
      next: (res) => {
        const data = res?.data || (res as any);
        if (data) {
          this.patientForm.patchValue({
            firstName: data.firstName || '',
            lastName: data.lastName || '',
            email: data.email || '',
            phoneNumber: data.phoneNumber || '',
            dateOfBirth: data.dateOfBirth || ''
          });
          this.cdr.markForCheck();
        }
      },
      error: (err) => {
        console.warn('Could not load patient details', err);
        this.cdr.markForCheck();
      }
    });
  }

  onSubmit(): void {
    if (this.patientForm.invalid) {
      this.patientForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMsg = '';
    this.successMsg = '';
    this.cdr.markForCheck();

    const payload = this.patientForm.value;

    if (this.isEditMode && this.patientId) {
      this.patientService.updatePatient(this.patientId, payload).subscribe({
        next: () => {
          this.submitting = false;
          this.successMsg = 'Patient updated successfully!';
          this.cdr.markForCheck();
          setTimeout(() => this.router.navigate(['/patients']), 1000);
        },
        error: (err) => {
          this.submitting = false;
          this.errorMsg = 'Failed to update patient: Backend API is pending or unreachable.';
          this.cdr.markForCheck();
        }
      });
    } else {
      this.patientService.createPatient(payload).subscribe({
        next: () => {
          this.submitting = false;
          this.successMsg = 'Patient registered successfully!';
          this.cdr.markForCheck();
          setTimeout(() => this.router.navigate(['/patients']), 1000);
        },
        error: (err) => {
          this.submitting = false;
          this.errorMsg = 'Failed to create patient: Backend POST /api/patients is pending or unreachable.';
          this.cdr.markForCheck();
        }
      });
    }
  }
}
