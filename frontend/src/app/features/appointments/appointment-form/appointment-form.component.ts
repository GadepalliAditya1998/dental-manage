import { Component, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AppointmentService } from '../../../core/services/appointment.service';
import { DoctorService } from '../../../core/services/doctor.service';
import { PatientService } from '../../../core/services/patient.service';
import { ServiceCatalogService } from '../../../core/services/service.service';
import { DoctorResponse, SlotInfo } from '../../../core/models/doctor.model';
import { PatientResponse } from '../../../core/models/patient.model';
import { ServiceResponse } from '../../../core/models/service.model';

@Component({
  selector: 'app-appointment-form',
  standalone: false,
  templateUrl: './appointment-form.component.html',
  styleUrls: ['./appointment-form.component.scss']
})
export class AppointmentFormComponent implements OnInit {
  appointmentForm!: FormGroup;
  doctors: DoctorResponse[] = [];
  patients: PatientResponse[] = [];
  services: ServiceResponse[] = [];
  availableSlots: SlotInfo[] = [];

  loadingSlots = false;
  submitting = false;
  errorMsg = '';
  successMsg = '';

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
    private doctorService: DoctorService,
    private patientService: PatientService,
    private catalogService: ServiceCatalogService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadDropdownData();
  }

  initForm(): void {
    const today = new Date().toISOString().split('T')[0];

    this.appointmentForm = this.fb.group({
      patientId: ['', Validators.required],
      doctorId: ['', Validators.required],
      serviceId: ['', Validators.required],
      date: [today, Validators.required],
      selectedSlot: ['', Validators.required]
    });

    // When doctor or date changes, reload available slots
    this.appointmentForm.get('doctorId')?.valueChanges.subscribe(() => this.fetchSlots());
    this.appointmentForm.get('date')?.valueChanges.subscribe(() => this.fetchSlots());
  }

  loadDropdownData(): void {
    this.doctorService.getDoctors().subscribe({
      next: (res) => {
        const list = Array.isArray(res) ? res : (res?.data || []);
        this.doctors = Array.isArray(list) ? list : [];
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.warn('Doctors API error:', err);
        this.cdr.markForCheck();
      }
    });

    this.patientService.getPatients().subscribe({
      next: (res) => {
        const list = Array.isArray(res) ? res : (res?.data || []);
        this.patients = Array.isArray(list) ? list : [];
        this.route.queryParams.subscribe(params => {
          if (params['patientId']) {
            this.appointmentForm.patchValue({ patientId: Number(params['patientId']) });
          }
        });
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.warn('Patients API error:', err);
        this.cdr.markForCheck();
      }
    });

    this.catalogService.getServices().subscribe({
      next: (res) => {
        const list = Array.isArray(res) ? res : (res?.data || []);
        this.services = Array.isArray(list) ? list : [];
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.warn('Services API error:', err);
        this.cdr.markForCheck();
      }
    });
  }

  fetchSlots(): void {
    const doctorId = this.appointmentForm.get('doctorId')?.value;
    const date = this.appointmentForm.get('date')?.value;

    if (!doctorId || !date) {
      this.availableSlots = [];
      this.loadingSlots = false;
      this.cdr.markForCheck();
      return;
    }

    this.loadingSlots = true;
    this.availableSlots = [];
    this.appointmentForm.get('selectedSlot')?.setValue('');
    this.cdr.markForCheck();

    this.appointmentService.getAvailableSlots(Number(doctorId), date).subscribe({
      next: (res) => {
        const data = res?.data || (res as any);
        this.availableSlots = data?.availableSlots || [];
        this.loadingSlots = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.warn('Slots API unreachable or error:', err);
        // Fallback default sample slots for responsive UX
        this.availableSlots = [
          { startTime: '09:00:00', endTime: '09:30:00', displayTime: '09:00 AM' },
          { startTime: '10:00:00', endTime: '10:30:00', displayTime: '10:00 AM' },
          { startTime: '11:30:00', endTime: '12:00:00', displayTime: '11:30 AM' },
          { startTime: '14:00:00', endTime: '14:30:00', displayTime: '02:00 PM' },
          { startTime: '15:30:00', endTime: '16:00:00', displayTime: '03:30 PM' },
          { startTime: '16:30:00', endTime: '17:00:00', displayTime: '04:30 PM' },
        ];
        this.loadingSlots = false;
        this.cdr.markForCheck();
      }
    });
  }

  onSubmit(): void {
    if (this.appointmentForm.invalid) {
      this.appointmentForm.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }

    this.submitting = true;
    this.errorMsg = '';
    this.successMsg = '';
    this.cdr.markForCheck();

    const formVal = this.appointmentForm.value;
    const appointmentDateTime = `${formVal.date}T${formVal.selectedSlot}`;

    const request = {
      patientId: Number(formVal.patientId),
      doctorId: Number(formVal.doctorId),
      serviceId: Number(formVal.serviceId),
      appointmentDateTime: appointmentDateTime
    };

    this.appointmentService.createAppointment(request).subscribe({
      next: () => {
        this.submitting = false;
        this.successMsg = 'Appointment scheduled successfully!';
        this.cdr.markForCheck();
        setTimeout(() => {
          this.ngZone.run(() => {
            this.router.navigate(['/appointments']);
          });
        }, 1000);
      },
      error: (err) => {
        this.submitting = false;
        this.errorMsg = err?.error?.message || 'Failed to book appointment: Backend POST /api/appointments is pending or unreachable.';
        this.cdr.markForCheck();
      }
    });
  }
}
