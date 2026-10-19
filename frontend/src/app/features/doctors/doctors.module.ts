import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../../shared/shared.module';
import { DoctorsRoutingModule } from './doctors-routing.module';
import { DoctorListComponent } from './doctor-list/doctor-list.component';
import { DoctorFormComponent } from './doctor-form/doctor-form.component';
import { DoctorAvailabilityComponent } from './doctor-availability/doctor-availability.component';

@NgModule({
  declarations: [
    DoctorListComponent,
    DoctorFormComponent,
    DoctorAvailabilityComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    DoctorsRoutingModule
  ]
})
export class DoctorsModule {}
