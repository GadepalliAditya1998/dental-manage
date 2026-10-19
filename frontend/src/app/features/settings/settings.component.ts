import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ClinicSettingsService } from '../../core/services/clinic-settings.service';
import { ClinicSettingsResponse } from '../../core/models/clinic-settings.model';

@Component({
  selector: 'app-settings',
  standalone: false,
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  settingsForm!: FormGroup;
  loading = false;
  saving = false;
  successMsg = '';
  errorMsg = '';

  allDays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  selectedDays: string[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

  constructor(
    private fb: FormBuilder,
    private settingsService: ClinicSettingsService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadSettings();
  }

  initForm(): void {
    this.settingsForm = this.fb.group({
      clinicName: ['Smile Dental Clinic', [Validators.required]],
      clinicPhone: ['+1-555-DENTAL-1', [Validators.required]],
      receptionistPhone: ['+1-555-RECEPT-1'],
      openingTime: ['09:00', [Validators.required]],
      closingTime: ['18:00', [Validators.required]],
      retellAgentId: ['agent_dental_inbound_v1'],
      timezone: ['Asia/Kolkata', [Validators.required]]
    });
  }

  loadSettings(): void {
    this.loading = true;
    this.settingsService.getSettings().subscribe({
      next: (res) => {
        if (res?.data) {
          const s = res.data;
          this.settingsForm.patchValue({
            clinicName: s.clinicName,
            clinicPhone: s.clinicPhone,
            receptionistPhone: s.receptionistPhone,
            openingTime: s.openingTime?.substring(0, 5) || '09:00',
            closingTime: s.closingTime?.substring(0, 5) || '18:00',
            retellAgentId: s.retellAgentId,
            timezone: s.timezone || 'Asia/Kolkata'
          });
          if (s.workingDays) {
            this.selectedDays = s.workingDays.split(',').map(d => d.trim().toUpperCase());
          }
        }
        this.loading = false;
      },
      error: (err) => {
        console.warn('Settings API error:', err);
        this.loading = false;
      }
    });
  }

  toggleDay(day: string): void {
    if (this.selectedDays.includes(day)) {
      this.selectedDays = this.selectedDays.filter(d => d !== day);
    } else {
      this.selectedDays.push(day);
    }
  }

  onSubmit(): void {
    if (this.settingsForm.invalid) {
      this.settingsForm.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.successMsg = '';
    this.errorMsg = '';

    const formVal = this.settingsForm.value;
    const payload = {
      clinicName: formVal.clinicName,
      clinicPhone: formVal.clinicPhone,
      receptionistPhone: formVal.receptionistPhone,
      openingTime: formVal.openingTime.length === 5 ? `${formVal.openingTime}:00` : formVal.openingTime,
      closingTime: formVal.closingTime.length === 5 ? `${formVal.closingTime}:00` : formVal.closingTime,
      workingDays: this.selectedDays.join(','),
      retellAgentId: formVal.retellAgentId,
      timezone: formVal.timezone
    };

    this.settingsService.updateSettings(payload).subscribe({
      next: () => {
        this.saving = false;
        this.successMsg = 'Clinic configuration saved successfully!';
      },
      error: () => {
        this.saving = false;
        this.errorMsg = 'Failed to update settings: Backend PUT /api/settings is pending or unreachable.';
      }
    });
  }
}
