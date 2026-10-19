import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../../shared/shared.module';
import { AppointmentsRoutingModule } from './appointments-routing.module';
import { AppointmentListComponent } from './appointment-list/appointment-list.component';
import { AppointmentFormComponent } from './appointment-form/appointment-form.component';

@NgModule({
  declarations: [
    AppointmentListComponent,
    AppointmentFormComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    AppointmentsRoutingModule
  ]
})
export class AppointmentsModule {}
