import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AuthComponent } from './auth/auth/auth.component';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SharedModule } from './shared/shared.module';
import { MyInvoicesComponent } from './my-invoices/my-invoices.component';
import { ProfileComponent } from './profile/profile.component';
import { PendingApprovalsComponent } from './pending-approvals/pending-approvals.component';
import { ApprovalHistoryComponent } from './approval-history/approval-history.component';
import { ApprovedInvoicesComponent } from './approved-invoices/approved-invoices.component';
import { PaymentReportsComponent } from './payment-reports/payment-reports.component';
import { ReportsComponent } from './reports/reports.component';
import { AuditLogsComponent } from './audit-logs/audit-logs.component';
import { SettingsComponent } from './settings/settings.component';
import { ChatbotComponent } from './chatbot/chatbot.component';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { ErrorInterceptor } from './interceptors/error.interceptor';

@NgModule({
  declarations: [
    AppComponent,
    AuthComponent,
    LoginComponent,
    RegisterComponent,
    DashboardComponent,
    MyInvoicesComponent,
    ProfileComponent,
    PendingApprovalsComponent,
    ApprovalHistoryComponent,
    ApprovedInvoicesComponent,
    PaymentReportsComponent,
    ReportsComponent,
    AuditLogsComponent,
    SettingsComponent,
    ChatbotComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    HttpClientModule,
    ReactiveFormsModule,
    FormsModule,
    SharedModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }

