import { Component, Input, Output, EventEmitter } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { ContextUser } from '../../../core/models/auth.model';

@Component({
  selector: 'app-navbar',
  standalone: false,
  template: `
    <header class="navbar navbar-expand-lg navbar-dark bg-teal-gradient sticky-top px-3 py-2 shadow-sm">
      <div class="container-fluid">
        <button class="btn btn-link text-white me-3 d-lg-none p-0" (click)="toggleSidebar.emit()">
          <i class="bi bi-list fs-3"></i>
        </button>

        <a class="navbar-brand d-flex align-items-center fw-bold" routerLink="/dashboard">
          <i class="bi bi-heart-pulse-fill me-2 fs-4 text-warning"></i>
          <span>Smile Dental Clinic</span>
        </a>

        <div class="ms-auto d-flex align-items-center">
          <!-- Live WebSocket Status -->
          <span class="badge rounded-pill me-3 d-none d-sm-inline-flex align-items-center"
                [ngClass]="wsConnected ? 'bg-success-subtle text-success border border-success' : 'bg-secondary-subtle text-muted border'">
            <i class="bi bi-circle-fill me-1" style="font-size: 0.5rem;"></i>
            {{ wsConnected ? 'Live Feed Connected' : 'Feed Offline' }}
          </span>

          <!-- Quick Action: Book Appointment -->
          <a routerLink="/appointments/new" class="btn btn-light btn-sm fw-semibold text-teal shadow-sm me-3 d-none d-md-inline-block">
            <i class="bi bi-plus-lg me-1"></i> Book Appointment
          </a>

          <!-- User Profile & Sign Out -->
          <div class="d-flex align-items-center">
            <div class="d-flex align-items-center me-2 text-white">
              <span class="rounded-circle bg-white text-teal fw-bold d-flex align-items-center justify-content-center me-2"
                    style="width: 34px; height: 34px; font-size: 0.85rem;">
                {{ userInitials }}
              </span>
              <div class="d-none d-md-block text-start lh-sm">
                <div class="small fw-bold">{{ contextUser?.name || 'Staff' }}</div>
                <div class="extra-small text-white-50 text-uppercase">
                  <span class="badge bg-white text-teal py-0 px-1">{{ contextUser?.role || 'Staff' }}</span>
                </div>
              </div>
            </div>

            <!-- Logout Button -->
            <button class="btn btn-sm btn-outline-light ms-2" (click)="onLogout()" title="Sign Out">
              <i class="bi bi-box-arrow-right me-1"></i>
              <span class="d-none d-sm-inline">Sign Out</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  `,
  styles: [`
    .navbar-brand {
      font-size: 1.15rem;
      letter-spacing: 0.2px;
    }
    .extra-small {
      font-size: 0.7rem;
    }
  `]
})
export class NavbarComponent {
  @Input() wsConnected = false;
  @Input() contextUser: ContextUser | null = null;
  @Output() toggleSidebar = new EventEmitter<void>();

  constructor(private authService: AuthService) {}

  get userInitials(): string {
    if (!this.contextUser?.name) return 'U';
    const name = this.contextUser.name.trim();
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  }

  onLogout(): void {
    this.authService.logout();
  }
}
