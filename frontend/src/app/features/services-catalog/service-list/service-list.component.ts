import { Component, OnInit } from '@angular/core';
import { ServiceCatalogService } from '../../../core/services/service.service';
import { ServiceResponse } from '../../../core/models/service.model';

@Component({
  selector: 'app-service-list',
  standalone: false,
  templateUrl: './service-list.component.html',
  styleUrls: ['./service-list.component.scss']
})
export class ServiceListComponent implements OnInit {
  services: ServiceResponse[] = [];
  loading = false;
  errorMessage = '';

  constructor(private catalogService: ServiceCatalogService) {}

  ngOnInit(): void {
    this.loadServices();
  }

  loadServices(): void {
    this.loading = true;
    this.errorMessage = '';

    this.catalogService.getServices().subscribe({
      next: (res) => {
        this.services = res?.data || [];
        this.loading = false;
      },
      error: (err) => {
        console.warn('Services API error:', err);
        this.errorMessage = 'Backend endpoint GET /api/services is pending or unreachable.';
        this.loading = false;
      }
    });
  }

  toggleActive(service: ServiceResponse): void {
    const updated = {
      name: service.name,
      description: service.description,
      durationMinutes: service.durationMinutes,
      price: service.price,
      isActive: !service.isActive
    };

    this.catalogService.updateService(service.id, updated).subscribe({
      next: () => {
        service.isActive = !service.isActive;
      },
      error: () => alert('Failed to update service. Backend API may be unreachable.')
    });
  }

  deleteService(id: number): void {
    if (!confirm('Are you sure you want to delete this treatment service?')) return;
    this.catalogService.deleteService(id).subscribe({
      next: () => {
        this.services = this.services.filter(s => s.id !== id);
      },
      error: () => alert('Failed to delete service. Backend API may be unreachable.')
    });
  }
}
