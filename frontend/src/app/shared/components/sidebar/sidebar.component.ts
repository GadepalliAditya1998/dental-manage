import { Component, Input } from '@angular/core';
import { ContextUser } from '../../../core/models/auth.model';

@Component({
  selector: 'app-sidebar',
  standalone: false,
  template: `
    <nav class="sidebar bg-white border-end d-flex flex-column py-3" [class.show-mobile]="isOpen">
      <div class="px-3 mb-3 d-flex justify-content-between align-items-center">
        <span class="text-uppercase text-muted fw-bold extra-small" style="letter-spacing: 0.8px; font-size: 0.75rem;">
          {{ isDoctor ? 'Practitioner Portal' : 'Clinic Desk Portal' }}
        </span>
        <span class="badge" [ngClass]="isDoctor ? 'bg-primary-subtle text-primary' : 'bg-info-subtle text-info-emphasis'">
          {{ contextUser?.role || 'Staff' }}
        </span>
      </div>

      <ul class="nav nav-pills flex-column px-2 mb-auto gap-1">
        <!-- Dashboard (Common) -->
        <li class="nav-item">
          <a routerLink="/dashboard" routerLinkActive="active" class="nav-link text-dark d-flex align-items-center">
            <i class="bi bi-grid-1x2-fill me-3 fs-5"></i>
            <span>Dashboard</span>
          </a>
        </li>

        <!-- Appointments (Common) -->
        <li class="nav-item">
          <a routerLink="/appointments" routerLinkActive="active" class="nav-link text-dark d-flex align-items-center">
            <i class="bi bi-calendar2-check-fill me-3 fs-5"></i>
            <span>{{ isDoctor ? 'My Appointments' : 'All Appointments' }}</span>
          </a>
        </li>

        <!-- Patients (Common) -->
        <li class="nav-item">
          <a routerLink="/patients" routerLinkActive="active" class="nav-link text-dark d-flex align-items-center">
            <i class="bi bi-people-fill me-3 fs-5"></i>
            <span>Patients</span>
          </a>
        </li>

        <!-- Doctor Only: My Availability -->
        <li class="nav-item" *ngIf="isDoctor && contextUser?.doctorId">
          <a [routerLink]="['/doctors', contextUser!.doctorId, 'availability']" routerLinkActive="active" class="nav-link text-dark d-flex align-items-center">
            <i class="bi bi-clock-history me-3 fs-5"></i>
            <span>My Availability</span>
          </a>
        </li>

        <!-- Receptionist Only: Doctors Management -->
        <li class="nav-item" *ngIf="!isDoctor">
          <a routerLink="/doctors" routerLinkActive="active" class="nav-link text-dark d-flex align-items-center">
            <i class="bi bi-person-badge-fill me-3 fs-5"></i>
            <span>Doctors Directory</span>
          </a>
        </li>

        <!-- Treatments Catalog (Common) -->
        <li class="nav-item">
          <a routerLink="/services" routerLinkActive="active" class="nav-link text-dark d-flex align-items-center">
            <i class="bi bi-bandaid-fill me-3 fs-5"></i>
            <span>Treatments Catalog</span>
          </a>
        </li>

        <!-- Receptionist Only: AI Call Logs -->
        <li class="nav-item" *ngIf="!isDoctor">
          <a routerLink="/calls" routerLinkActive="active" class="nav-link text-dark d-flex align-items-center">
            <i class="bi bi-telephone-inbound-fill me-3 fs-5"></i>
            <span>AI Voice Calls</span>
          </a>
        </li>
      </ul>

      <!-- Receptionist Only: Administration Section -->
      <ng-container *ngIf="!isDoctor">
        <hr class="mx-3 my-3 text-secondary">

        <div class="px-3 mb-2">
          <span class="text-uppercase text-muted fw-bold extra-small" style="letter-spacing: 0.8px; font-size: 0.75rem;">
            Administration
          </span>
        </div>

        <ul class="nav nav-pills flex-column px-2 gap-1">
          <li class="nav-item">
            <a routerLink="/settings" routerLinkActive="active" class="nav-link text-dark d-flex align-items-center">
              <i class="bi bi-gear-fill me-3 fs-5"></i>
              <span>Clinic Settings</span>
            </a>
          </li>
        </ul>
      </ng-container>
    </nav>
  `,
  styles: [`
    .sidebar {
      width: 250px;
      min-height: calc(100vh - 56px);
    }
    .nav-link {
      border-radius: 0.5rem;
      padding: 0.65rem 1rem;
      font-weight: 500;
      color: #475569 !important;
      transition: all 0.2s ease;
    }
    .nav-link:hover {
      background-color: #f1f5f9;
      color: #0d9488 !important;
    }
    .nav-link.active {
      background-color: #ccfbf1 !important;
      color: #0f766e !important;
      font-weight: 600;
    }
    .nav-link.active i {
      color: #0d9488 !important;
    }
    @media (max-width: 991.98px) {
      .sidebar {
        position: fixed;
        top: 56px;
        left: -260px;
        z-index: 1040;
        transition: left 0.3s ease;
        box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
      }
      .sidebar.show-mobile {
        left: 0;
      }
    }
  `]
})
export class SidebarComponent {
  @Input() isOpen = false;
  @Input() contextUser: ContextUser | null = null;

  get isDoctor(): boolean {
    return this.contextUser?.role?.toUpperCase() === 'DOCTOR';
  }
}
