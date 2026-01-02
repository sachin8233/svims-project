import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { VendorListComponent } from './vendor-list/vendor-list.component';
import { VendorFormComponent } from './vendor-form/vendor-form.component';
import { SharedModule } from '../shared/shared.module';

const routes = [
  { path: '', component: VendorListComponent },
  { path: 'new', component: VendorFormComponent },
  { path: 'edit/:id', component: VendorFormComponent }
];

@NgModule({
  declarations: [
    VendorListComponent,
    VendorFormComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule.forChild(routes),
    SharedModule
  ]
})
export class VendorsModule { }

