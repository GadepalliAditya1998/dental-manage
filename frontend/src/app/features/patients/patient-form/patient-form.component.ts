import { Component, OnInit } from '@angular/core';
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
    private router: Router
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
      email: ['', [Validators.email]],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^[+]?[0-9\s-]{7,15}$/)]]
    });
  }

  loadPatientData(id: number): void {
    this.patientService.getPatientById(id).subscribe({
      next: (res) => {
        if (res?.data) {
          this.patientForm.patchValue({
            firstName: res.data.firstName,
            lastName: res.data.lastName,
            email: res.data.email,
            phoneNumber: res.data.phoneNumber
          });
        }
      },
      error: (err) => console.warn('Could not load patient details', err)
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

    const payload = this.patientForm.value;

    if (this.isEditMode && this.patientId) {
      this.patientService.updatePatient(this.patientId, payload).subscribe({
        next: () => {
          this.submitting = false;
          this.successMsg = 'Patient updated successfully!';
          setTimeout(() => this.router.navigate(['/patients']), 1000);
        },
        error: (err) => {
          this.submitting = false;
          this.errorMsg = 'Failed to update patient: Backend API is pending or unreachable.';
        }
      });
    } else {
      this.patientService.createPatient(payload).subscribe({
        next: () => {
          this.submitting = false;
          this.successMsg = 'Patient registered successfully!';
          setTimeout(() => this.router.navigate(['/patients']), 1000);
        },
        error: (err) => {
          this.submitting = false;
          this.errorMsg = 'Failed to create patient: Backend POST /api/patients is pending or unreachable.';
        }
      });
    }
  }
}
