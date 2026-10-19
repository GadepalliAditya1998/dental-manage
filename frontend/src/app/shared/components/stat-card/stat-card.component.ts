import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  standalone: false,
  template: `
    <div class="card card-custom h-100 border-0 p-3">
      <div class="d-flex align-items-center justify-content-between">
        <div>
          <span class="text-uppercase text-muted fw-bold small" style="letter-spacing: 0.5px;">{{ title }}</span>
          <h3 class="fw-bold mt-2 mb-1" [ngClass]="valueClass">{{ value }}</h3>
          <p class="mb-0 text-muted small" *ngIf="subtitle">
            <i class="bi bi-info-circle me-1"></i>{{ subtitle }}
          </p>
        </div>
        <div class="p-3 rounded-circle d-flex align-items-center justify-content-center"
             [ngStyle]="{'background-color': iconBgColor, 'width': '52px', 'height': '52px'}">
          <i [class]="'bi ' + icon" [style.color]="iconColor" style="font-size: 1.5rem;"></i>
        </div>
      </div>
    </div>
  `
})
export class StatCardComponent {
  @Input() title = '';
  @Input() value: string | number = '';
  @Input() subtitle?: string;
  @Input() icon = 'bi-bar-chart';
  @Input() iconColor = '#0d9488';
  @Input() iconBgColor = '#ccfbf1';
  @Input() valueClass = 'text-dark';
}
