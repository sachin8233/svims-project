import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthComponent } from './auth/auth/auth.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { MyInvoicesComponent } from './my-invoices/my-invoices.component';
import { ProfileComponent } from './profile/profile.component';
import { PendingApprovalsComponent } from './pending-approvals/pending-approvals.component';
import { ApprovalHistoryComponent } from './approval-history/approval-history.component';
import { ApprovedInvoicesComponent } from './approved-invoices/approved-invoices.component';
import { PaymentReportsComponent } from './payment-reports/payment-reports.component';
import { ReportsComponent } from './reports/reports.component';
import { AuditLogsComponent } from './audit-logs/audit-logs.component';
import { SettingsComponent } from './settings/settings.component';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';

const routes: Routes = [
  { path: '', redirectTo: '/auth', pathMatch: 'full' },
  { path: 'auth', component: AuthComponent },
  { path: 'login', redirectTo: '/auth', pathMatch: 'full' },
  { path: 'register', redirectTo: '/auth', pathMatch: 'full' },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'vendors',
    loadChildren: () => import('./vendors/vendors.module').then(m => m.VendorsModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'invoices',
    loadChildren: () => import('./invoices/invoices.module').then(m => m.InvoicesModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'my-invoices',
    component: MyInvoicesComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'profile',
    component: ProfileComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'pending-approvals',
    component: PendingApprovalsComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] }
  },
  {
    path: 'approval-history',
    component: ApprovalHistoryComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] }
  },
  {
    path: 'approved-invoices',
    component: ApprovedInvoicesComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN', 'ROLE_FINANCE'] }
  },
  {
    path: 'payments',
    loadChildren: () => import('./payments/payments.module').then(m => m.PaymentsModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN', 'ROLE_FINANCE'] }
  },
  {
    path: 'payment-reports',
    component: PaymentReportsComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN', 'ROLE_FINANCE'] }
  },
  {
    path: 'users',
    loadChildren: () => import('./users/users.module').then(m => m.UsersModule),
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN'] }
  },
  {
    path: 'reports',
    component: ReportsComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN'] }
  },
  {
    path: 'audit-logs',
    component: AuditLogsComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN'] }
  },
  {
    path: 'settings',
    component: SettingsComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN'] }
  },
  { path: '**', redirectTo: '/auth' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

