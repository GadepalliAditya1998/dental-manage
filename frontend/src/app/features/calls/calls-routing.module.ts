import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CallListComponent } from './call-list/call-list.component';

const routes: Routes = [
  { path: '', component: CallListComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class CallsRoutingModule {}
