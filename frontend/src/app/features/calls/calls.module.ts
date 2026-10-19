import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../../shared/shared.module';
import { CallsRoutingModule } from './calls-routing.module';
import { CallListComponent } from './call-list/call-list.component';

@NgModule({
  declarations: [
    CallListComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    CallsRoutingModule
  ]
})
export class CallsModule {}
