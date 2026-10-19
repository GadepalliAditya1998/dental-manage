import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  standalone: false,
  template: `
    <div class="d-flex flex-column align-items-center justify-content-center py-5">
      <div class="spinner-border text-primary" role="status" style="width: 2.5rem; height: 2.5rem;">
        <span class="visually-hidden">Loading...</span>
      </div>
      <p class="text-muted mt-3 mb-0">{{ message }}</p>
    </div>
  `
})
export class LoadingSpinnerComponent {
  @Input() message = 'Loading data...';
}
