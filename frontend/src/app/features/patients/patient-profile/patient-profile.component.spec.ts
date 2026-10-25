import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { PatientProfileComponent } from './patient-profile.component';
import { PatientService } from '../../../core/services/patient.service';
import { AppointmentService } from '../../../core/services/appointment.service';
import { PatientsModule } from '../patients.module';

describe('PatientProfileComponent', () => {
  let component: PatientProfileComponent;
  let fixture: ComponentFixture<PatientProfileComponent>;

  const mockPatient = {
    id: 1,
    patientId: 'PAT-1-1',
    clinicId: 1,
    firstName: 'John',
    lastName: 'Doe',
    fullName: 'John Doe',
    username: 'JOHN@GMAIL.COM',
    password: '',
    email: 'JOHN@GMAIL.COM',
    phoneNumber: '155550199',
    dateOfBirth: '1990-05-15',
    isNewLead: true,
    whatsappOptOut: false,
    createdOn: '2026-10-23T17:00:04.912834',
    modifiedOn: '2026-10-23T17:00:04.912834'
  };

  const mockAppointment = {
    id: 101,
    clinicId: 1,
    patient: mockPatient,
    appointmentStart: '2026-10-26T10:00:00',
    appointmentEnd: '2026-10-26T10:30:00',
    status: 'SCHEDULED' as const,
    reason: 'Dental checkup',
    bookingSource: 'MANUAL' as const
  };

  let mockPatientService: any;
  let mockAppointmentService: any;

  beforeEach(async () => {
    mockPatientService = {
      getPatientById: () => of({ success: true, message: 'OK', data: mockPatient }),
      updatePatient: () => of({ success: true, message: 'OK', data: mockPatient })
    };

    mockAppointmentService = {
      getAppointments: () => of({ success: true, message: 'OK', data: [mockAppointment] }),
      updateStatus: () => of({ success: true, message: 'OK', data: mockAppointment })
    };

    await TestBed.configureTestingModule({
      imports: [PatientsModule, RouterModule.forRoot([])],
      providers: [
        { provide: PatientService, useValue: mockPatientService },
        { provide: AppointmentService, useValue: mockAppointmentService },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of({
              get: (key: string) => (key === 'id' ? '1' : null)
            }),
            queryParamMap: of({
              get: (key: string) => null
            })
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PatientProfileComponent);
    component = fixture.componentInstance;
  });

  it('should load patient details and clear patient loading state', () => {
    fixture.detectChanges();
    expect(component.loadingPatient).toBe(false);
    expect(component.patient).toBeTruthy();
    expect(component.patient?.firstName).toBe('John');
  });

  it('should load appointments and clear appointments loading state', () => {
    fixture.detectChanges();
    expect(component.loadingAppointments).toBe(false);
    expect(component.allAppointments.length).toBe(1);
    expect(component.displayedAppointments.length).toBe(1);
  });

  it('should categorize appointments safely', () => {
    fixture.detectChanges();
    expect(component.upcomingAppointments.length).toBe(1);
    expect(component.pastAppointments.length).toBe(0);
  });

  it('should clear loadingAppointments when appointments API fails', () => {
    mockAppointmentService.getAppointments = () => throwError(() => new Error('Failed to load'));
    component.loadAppointments();
    expect(component.loadingAppointments).toBe(false);
    expect(component.allAppointments.length).toBe(0);
    expect(component.displayedAppointments.length).toBe(0);
  });

  it('should switch active main tab between profile and appointments', () => {
    expect(component.activeMainTab).toBe('profile');
    component.setMainTab('appointments');
    expect(component.activeMainTab).toBe('appointments');
    component.setMainTab('profile');
    expect(component.activeMainTab).toBe('profile');
  });
});
