import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from './navbar/navbar.component';
import { ModalComponent } from './modal/modal.component';
import { ConfirmModalComponent } from './confirm-modal/confirm-modal.component';
import { InputModalComponent } from './input-modal/input-modal.component';

@NgModule({
  declarations: [
    NavbarComponent,
    ModalComponent,
    ConfirmModalComponent,
    InputModalComponent
  ],
  exports: [
    NavbarComponent,
    ModalComponent,
    ConfirmModalComponent,
    InputModalComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    FormsModule
  ]
})
export class SharedModule { }


