import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ServiceCatalogService } from '../../../core/services/service.service';

@Component({
  selector: 'app-service-form',
  standalone: false,
  templateUrl: './service-form.component.html',
  styleUrls: ['./service-form.component.scss']
})
export class ServiceFormComponent implements OnInit {
  serviceForm!: FormGroup;
  isEditMode = false;
  serviceId: number | null = null;
  submitting = false;
  errorMsg = '';
  successMsg = '';

  constructor(
    private fb: FormBuilder,
    private catalogService: ServiceCatalogService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.serviceId = Number(idParam);
      this.loadService(this.serviceId);
    }
  }

  initForm(): void {
    this.serviceForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      durationMinutes: [30, [Validators.required, Validators.min(5), Validators.max(480)]],
      price: [100.00, [Validators.required, Validators.min(0)]],
      isActive: [true]
    });
  }

  loadService(id: number): void {
    this.catalogService.getServiceById(id).subscribe({
      next: (res) => {
        if (res?.data) {
          this.serviceForm.patchValue({
            name: res.data.name,
            description: res.data.description,
            durationMinutes: res.data.durationMinutes,
            price: res.data.price,
            isActive: res.data.isActive ?? true
          });
        }
      },
      error: (err) => console.warn('Could not load service details', err)
    });
  }

  onSubmit(): void {
    if (this.serviceForm.invalid) {
      this.serviceForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMsg = '';
    this.successMsg = '';

    const payload = this.serviceForm.value;

    if (this.isEditMode && this.serviceId) {
      this.catalogService.updateService(this.serviceId, payload).subscribe({
        next: () => {
          this.submitting = false;
          this.successMsg = 'Service updated successfully!';
          setTimeout(() => this.router.navigate(['/services']), 1000);
        },
        error: () => {
          this.submitting = false;
          this.errorMsg = 'Failed to update service: Backend API is pending or unreachable.';
        }
      });
    } else {
      this.catalogService.createService(payload).subscribe({
        next: () => {
          this.submitting = false;
          this.successMsg = 'Service created successfully!';
          setTimeout(() => this.router.navigate(['/services']), 1000);
        },
        error: () => {
          this.submitting = false;
          this.errorMsg = 'Failed to create service: Backend POST /api/services is pending or unreachable.';
        }
      });
    }
  }
}
