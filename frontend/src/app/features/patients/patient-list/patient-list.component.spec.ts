import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PatientListComponent } from './patient-list.component';
import { PatientService } from '../../../core/services/patient.service';
import { PatientsModule } from '../patients.module';
import { RouterModule } from '@angular/router';

describe('PatientListComponent', () => {
  let component: PatientListComponent;
  let fixture: ComponentFixture<PatientListComponent>;

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

  const mockResponse = {
    success: true,
    message: 'Patients fetched successfully',
    data: [mockPatient]
  };

  let mockService: any;

  beforeEach(async () => {
    mockService = {
      getPatients: () => of(mockResponse),
      deletePatient: () => of({ success: true, message: '', data: undefined })
    };

    await TestBed.configureTestingModule({
      imports: [PatientsModule, RouterModule.forRoot([])],
      providers: [
        { provide: PatientService, useValue: mockService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PatientListComponent);
    component = fixture.componentInstance;
  });

  it('should load patients from ApiResponse and set loading to false', () => {
    fixture.detectChanges();
    expect(component.loading).toBe(false);
    expect(component.patients.length).toBe(1);
    expect(component.filteredPatients.length).toBe(1);
    expect(component.filteredPatients[0].firstName).toBe('John');
  });

  it('should load patients when response is directly an array and set loading to false', () => {
    mockService.getPatients = () => of([mockPatient]);
    component.loadPatients();
    expect(component.loading).toBe(false);
    expect(component.patients.length).toBe(1);
    expect(component.filteredPatients.length).toBe(1);
  });

  it('should filter patients by searchQuery matching fullName or patientId', () => {
    fixture.detectChanges();
    component.searchQuery = 'PAT-1';
    component.applyFilter();
    expect(component.filteredPatients.length).toBe(1);

    component.searchQuery = 'NonExistent';
    component.applyFilter();
    expect(component.filteredPatients.length).toBe(0);
  });

  it('should set loading to false and set errorMessage on error', () => {
    mockService.getPatients = () => throwError(() => new Error('API Error'));
    component.loadPatients();
    expect(component.loading).toBe(false);
    expect(component.errorMessage).toBeTruthy();
  });
});
