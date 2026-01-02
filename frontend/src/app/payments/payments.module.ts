import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { PaymentListComponent } from './payment-list/payment-list.component';
import { PaymentFormComponent } from './payment-form/payment-form.component';
import { SharedModule } from '../shared/shared.module';

const routes = [
  { path: '', component: PaymentListComponent },
  { path: 'new', component: PaymentFormComponent }
];

@NgModule({
  declarations: [
    PaymentListComponent,
    PaymentFormComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule.forChild(routes),
    SharedModule
  ]
})
export class PaymentsModule { }

