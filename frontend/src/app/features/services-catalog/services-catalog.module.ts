import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../../shared/shared.module';
import { ServicesCatalogRoutingModule } from './services-catalog-routing.module';
import { ServiceListComponent } from './service-list/service-list.component';
import { ServiceFormComponent } from './service-form/service-form.component';

@NgModule({
  declarations: [
    ServiceListComponent,
    ServiceFormComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    ServicesCatalogRoutingModule
  ]
})
export class ServicesCatalogModule {}
