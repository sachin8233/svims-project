import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { InvoiceListComponent } from './invoice-list/invoice-list.component';
import { InvoiceFormComponent } from './invoice-form/invoice-form.component';
import { InvoiceViewComponent } from './invoice-view/invoice-view.component';
import { SharedModule } from '../shared/shared.module';

const routes = [
  { path: '', component: InvoiceListComponent },
  { path: 'new', component: InvoiceFormComponent },
  { path: 'edit/:id', component: InvoiceFormComponent },
  { path: 'view/:id', component: InvoiceViewComponent }
];

@NgModule({
  declarations: [
    InvoiceListComponent,
    InvoiceFormComponent,
    InvoiceViewComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule.forChild(routes),
    SharedModule
  ]
})
export class InvoicesModule { }

