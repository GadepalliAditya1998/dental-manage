import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { NavbarComponent } from './components/navbar/navbar.component';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { LoadingSpinnerComponent } from './components/loading-spinner/loading-spinner.component';
import { StatCardComponent } from './components/stat-card/stat-card.component';
import { StatusBadgePipe } from './pipes/status-badge.pipe';
import { DurationPipe } from './pipes/duration.pipe';

@NgModule({
  declarations: [
    NavbarComponent,
    SidebarComponent,
    LoadingSpinnerComponent,
    StatCardComponent,
    StatusBadgePipe,
    DurationPipe
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule
  ],
  exports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    NavbarComponent,
    SidebarComponent,
    LoadingSpinnerComponent,
    StatCardComponent,
    StatusBadgePipe,
    DurationPipe
  ]
})
export class SharedModule {}
