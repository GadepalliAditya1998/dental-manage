import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ServiceListComponent } from './service-list/service-list.component';
import { ServiceFormComponent } from './service-form/service-form.component';

const routes: Routes = [
  { path: '', component: ServiceListComponent },
  { path: 'new', component: ServiceFormComponent },
  { path: ':id/edit', component: ServiceFormComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ServicesCatalogRoutingModule {}
