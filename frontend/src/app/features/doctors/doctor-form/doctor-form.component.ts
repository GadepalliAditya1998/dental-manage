import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DoctorService } from '../../../core/services/doctor.service';

@Component({
  selector: 'app-doctor-form',
  standalone: false,
  templateUrl: './doctor-form.component.html',
  styleUrls: ['./doctor-form.component.scss']
})
export class DoctorFormComponent implements OnInit {
  doctorForm!: FormGroup;
  isEditMode = false;
  doctorId: number | null = null;
  submitting = false;
  errorMsg = '';
  successMsg = '';

  specializations = [
    'General Dentistry',
    'Orthodontics',
    'Periodontics',
    'Endodontics',
    'Oral & Maxillofacial Surgery',
    'Pediatric Dentistry',
    'Prosthodontics'
  ];

  constructor(
    private fb: FormBuilder,
    private doctorService: DoctorService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.doctorId = Number(idParam);
      this.loadDoctor(this.doctorId);
    }
  }

  initForm(): void {
    this.doctorForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      specialization: ['General Dentistry'],
      email: ['', [Validators.email]],
      phoneNumber: [''],
      isActive: [true]
    });
  }

  loadDoctor(id: number): void {
    this.doctorService.getDoctorById(id).subscribe({
      next: (res) => {
        if (res?.data) {
          this.doctorForm.patchValue({
            firstName: res.data.firstName,
            lastName: res.data.lastName,
            specialization: res.data.specialization || 'General Dentistry',
            email: res.data.email,
            phoneNumber: res.data.phoneNumber,
            isActive: res.data.isActive ?? true
          });
        }
      },
      error: (err) => console.warn('Could not load doctor details', err)
    });
  }

  onSubmit(): void {
    if (this.doctorForm.invalid) {
      this.doctorForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMsg = '';
    this.successMsg = '';

    const payload = this.doctorForm.value;

    if (this.isEditMode && this.doctorId) {
      this.doctorService.updateDoctor(this.doctorId, payload).subscribe({
        next: () => {
          this.submitting = false;
          this.successMsg = 'Doctor updated successfully!';
          setTimeout(() => this.router.navigate(['/doctors']), 1000);
        },
        error: () => {
          this.submitting = false;
          this.errorMsg = 'Failed to update doctor: Backend API is pending or unreachable.';
        }
      });
    } else {
      this.doctorService.createDoctor(payload).subscribe({
        next: () => {
          this.submitting = false;
          this.successMsg = 'Doctor added successfully!';
          setTimeout(() => this.router.navigate(['/doctors']), 1000);
        },
        error: () => {
          this.submitting = false;
          this.errorMsg = 'Failed to create doctor: Backend POST /api/doctors is pending or unreachable.';
        }
      });
    }
  }
}
