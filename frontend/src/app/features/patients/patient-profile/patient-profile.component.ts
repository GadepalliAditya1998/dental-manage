import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PatientService } from '../../../core/services/patient.service';
import { AppointmentService } from '../../../core/services/appointment.service';
import { PatientRequest, PatientResponse } from '../../../core/models/patient.model';
import { AppointmentResponse, AppointmentStatus } from '../../../core/models/appointment.model';

export interface ParsedTranscriptLine {
  speaker: string;
  message: string;
  isAgent: boolean;
}

@Component({
  selector: 'app-patient-profile',
  standalone: false,
  templateUrl: './patient-profile.component.html',
  styleUrls: ['./patient-profile.component.scss']
})
export class PatientProfileComponent implements OnInit {
  patientId!: number;
  patient: PatientResponse | null = null;
  settingsForm!: FormGroup;

  allAppointments: AppointmentResponse[] = [];
  upcomingAppointments: AppointmentResponse[] = [];
  pastAppointments: AppointmentResponse[] = [];
  displayedAppointments: AppointmentResponse[] = [];

  activeMainTab: 'profile' | 'appointments' = 'profile';
  activeTab: 'upcoming' | 'past' | 'all' = 'upcoming';
  appointmentSearch = '';

  loadingPatient = false;
  loadingAppointments = false;
  savingSettings = false;
  updatingAppointmentStatus = false;

  successMessage = '';
  errorMessage = '';

  selectedAppointment: AppointmentResponse | null = null;
  copiedTranscript = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private patientService: PatientService,
    private appointmentService: AppointmentService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initForm();
    if (this.route.queryParamMap) {
      this.route.queryParamMap.subscribe(queryParams => {
        const tabParam = queryParams.get('tab');
        if (tabParam === 'profile' || tabParam === 'settings') {
          this.activeMainTab = 'profile';
        } else if (tabParam === 'appointments') {
          this.activeMainTab = 'appointments';
        }
        this.cdr.markForCheck();
      });
    }

    if (this.route.paramMap) {
      this.route.paramMap.subscribe(params => {
        const idParam = params.get('id');
        if (idParam && !isNaN(Number(idParam))) {
          this.patientId = Number(idParam);
          this.loadPatient();
          this.loadAppointments();
        } else {
          this.router.navigate(['/patients']);
        }
      });
    }
  }

  private initForm(): void {
    this.settingsForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', [Validators.required]],
      dateOfBirth: [''],
      whatsappOptOut: [false],
      isNewLead: [false]
    });
  }

  loadPatient(): void {
    this.loadingPatient = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.patientService.getPatientById(this.patientId).subscribe({
      next: (res) => {
        const patientData = res?.data || (res as any);
        this.patient = patientData || null;
        if (this.patient) {
          this.settingsForm.patchValue({
            firstName: this.patient.firstName || '',
            lastName: this.patient.lastName || '',
            email: this.patient.email || '',
            phoneNumber: this.patient.phoneNumber || '',
            dateOfBirth: this.patient.dateOfBirth || '',
            whatsappOptOut: !!this.patient.whatsappOptOut,
            isNewLead: !!this.patient.isNewLead
          });
        }
        this.loadingPatient = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error loading patient details:', err);
        this.errorMessage = 'Failed to load patient profile. Please verify that the patient exists.';
        this.loadingPatient = false;
        this.cdr.markForCheck();
      }
    });
  }

  loadAppointments(): void {
    this.loadingAppointments = true;
    this.cdr.markForCheck();

    this.appointmentService.getAppointments({ patientId: this.patientId }).subscribe({
      next: (res) => {
        const rawList = Array.isArray(res) ? res : (Array.isArray(res?.data) ? res.data : []);
        this.allAppointments = rawList;
        this.categorizeAppointments();
        this.filterAppointments();
        this.loadingAppointments = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.warn('Could not load appointments for patient:', err);
        this.allAppointments = [];
        this.upcomingAppointments = [];
        this.pastAppointments = [];
        this.displayedAppointments = [];
        this.loadingAppointments = false;
        this.cdr.markForCheck();
      }
    });
  }

  categorizeAppointments(): void {
    const now = new Date();
    const appts = Array.isArray(this.allAppointments) ? this.allAppointments : [];

    // Sort all by appointmentStart descending (most recent first)
    const sorted = [...appts].sort((a, b) => {
      const timeB = b?.appointmentStart ? new Date(b.appointmentStart).getTime() : 0;
      const timeA = a?.appointmentStart ? new Date(a.appointmentStart).getTime() : 0;
      return timeB - timeA;
    });

    this.upcomingAppointments = sorted
      .filter(a => {
        if (!a) return false;
        const start = a.appointmentStart || a.startTime;
        const apptDate = start ? new Date(start) : null;
        const isNotTerminal = a.status !== 'CANCELLED' && a.status !== 'COMPLETED' && a.status !== 'NO_SHOW';
        return (apptDate && apptDate >= now) || isNotTerminal;
      })
      .sort((a, b) => {
        const timeA = a?.appointmentStart ? new Date(a.appointmentStart).getTime() : 0;
        const timeB = b?.appointmentStart ? new Date(b.appointmentStart).getTime() : 0;
        return timeA - timeB;
      });

    this.pastAppointments = sorted.filter(a => {
      if (!a) return false;
      const start = a.appointmentStart || a.startTime;
      const apptDate = start ? new Date(start) : null;
      const isTerminal = a.status === 'COMPLETED' || a.status === 'CANCELLED' || a.status === 'NO_SHOW';
      return (apptDate && apptDate < now) || isTerminal;
    });
  }

  setMainTab(tab: 'appointments' | 'profile'): void {
    this.activeMainTab = tab;
    this.cdr.markForCheck();
  }

  setTab(tab: 'upcoming' | 'past' | 'all'): void {
    this.activeTab = tab;
    this.filterAppointments();
    this.cdr.markForCheck();
  }

  filterAppointments(): void {
    let sourceList: AppointmentResponse[] = [];
    if (this.activeTab === 'upcoming') {
      sourceList = this.upcomingAppointments;
    } else if (this.activeTab === 'past') {
      sourceList = this.pastAppointments;
    } else {
      sourceList = this.allAppointments;
    }

    if (!this.appointmentSearch.trim()) {
      this.displayedAppointments = sourceList;
      this.cdr.markForCheck();
      return;
    }

    const q = this.appointmentSearch.toLowerCase().trim();
    this.displayedAppointments = sourceList.filter(a =>
      a.service?.name?.toLowerCase().includes(q) ||
      a.doctor?.firstName?.toLowerCase().includes(q) ||
      a.doctor?.lastName?.toLowerCase().includes(q) ||
      a.doctor?.specialization?.toLowerCase().includes(q) ||
      a.reason?.toLowerCase().includes(q) ||
      a.reasonForAppointment?.toLowerCase().includes(q) ||
      a.status?.toLowerCase().includes(q) ||
      a.bookingSource?.toLowerCase().includes(q)
    );
    this.cdr.markForCheck();
  }

  saveSettings(): void {
    if (this.settingsForm.invalid) {
      this.settingsForm.markAllAsTouched();
      return;
    }

    this.savingSettings = true;
    this.successMessage = '';
    this.errorMessage = '';
    this.cdr.markForCheck();

    const formVal = this.settingsForm.value;
    const updateReq: PatientRequest = {
      firstName: formVal.firstName?.trim(),
      lastName: formVal.lastName?.trim(),
      email: formVal.email?.trim(),
      phoneNumber: formVal.phoneNumber?.trim(),
      dateOfBirth: formVal.dateOfBirth || undefined,
      whatsappOptOut: formVal.whatsappOptOut,
      isNewLead: formVal.isNewLead
    };

    this.patientService.updatePatient(this.patientId, updateReq).subscribe({
      next: (res) => {
        this.patient = res?.data || (res as any);
        this.savingSettings = false;
        this.successMessage = 'Patient profile settings updated successfully.';
        this.cdr.markForCheck();
        setTimeout(() => {
          this.successMessage = '';
          this.cdr.markForCheck();
        }, 5000);
      },
      error: (err) => {
        console.error('Failed to update patient settings:', err);
        this.errorMessage = err.error?.message || 'Failed to update patient settings. Please try again.';
        this.savingSettings = false;
        this.cdr.markForCheck();
      }
    });
  }

  resetSettings(): void {
    if (this.patient) {
      this.settingsForm.patchValue({
        firstName: this.patient.firstName || '',
        lastName: this.patient.lastName || '',
        email: this.patient.email || '',
        phoneNumber: this.patient.phoneNumber || '',
        dateOfBirth: this.patient.dateOfBirth || '',
        whatsappOptOut: !!this.patient.whatsappOptOut,
        isNewLead: !!this.patient.isNewLead
      });
      this.successMessage = '';
      this.errorMessage = '';
      this.cdr.markForCheck();
    }
  }

  openAppointmentModal(appt: AppointmentResponse): void {
    this.selectedAppointment = appt;
    this.copiedTranscript = false;
    this.cdr.markForCheck();
  }

  closeAppointmentModal(): void {
    this.selectedAppointment = null;
    this.copiedTranscript = false;
    this.cdr.markForCheck();
  }

  updateSelectedAppointmentStatus(newStatus: AppointmentStatus): void {
    if (!this.selectedAppointment) return;

    this.updatingAppointmentStatus = true;
    this.cdr.markForCheck();
    this.appointmentService.updateStatus(this.selectedAppointment.id, newStatus).subscribe({
      next: (res) => {
        if (this.selectedAppointment) {
          this.selectedAppointment.status = res?.data?.status || newStatus;
        }
        this.loadAppointments();
        this.updatingAppointmentStatus = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to update status:', err);
        alert('Could not update appointment status. Please try again.');
        this.updatingAppointmentStatus = false;
        this.cdr.markForCheck();
      }
    });
  }

  parseTranscript(rawTranscript: string | undefined): ParsedTranscriptLine[] {
    if (!rawTranscript) return [];

    const lines = rawTranscript.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
    const result: ParsedTranscriptLine[] = [];

    for (const line of lines) {
      const match = line.match(/^(Agent|AI Assistant|Assistant|Bot|Dentist|Doctor|User|Caller|Patient):\s*(.*)$/i);
      if (match) {
        const speaker = match[1];
        const message = match[2];
        const isAgent = /^(Agent|AI Assistant|Assistant|Bot)$/i.test(speaker);
        result.push({ speaker, message, isAgent });
      } else {
        result.push({ speaker: '', message: line, isAgent: false });
      }
    }

    return result;
  }

  copyTranscript(text: string | undefined): void {
    if (!text) return;
    navigator.clipboard.writeText(text).then(() => {
      this.copiedTranscript = true;
      this.cdr.markForCheck();
      setTimeout(() => {
        this.copiedTranscript = false;
        this.cdr.markForCheck();
      }, 2500);
    });
  }

  isBookedFromCall(appt: AppointmentResponse): boolean {
    return appt.bookingSource === 'AI_AGENT' || !!appt.callRecord;
  }
}
