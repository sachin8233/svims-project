import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { InvoiceService } from '../services/invoice.service';
import { VendorService } from '../services/vendor.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  currentUser: any;
  stats = {
    totalVendors: 0,
    paidInvoices: 0,
    pendingInvoices: 0,
    overdueInvoices: 0
  };
  recentInvoices: any[] = [];

  constructor(
    private authService: AuthService,
    private invoiceService: InvoiceService,
    private vendorService: VendorService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    // Only load vendor stats for ADMIN, MANAGER, FINANCE
    if (this.hasAdminRole() || this.hasManagerRole() || this.hasFinanceRole()) {
      this.vendorService.getAllVendors().subscribe(vendors => {
        this.stats.totalVendors = vendors.length;
      });
    }

    this.invoiceService.getAllInvoices().subscribe(invoices => {
      // For USER role, show only their invoices (for now show all, can filter by user later)
      const filteredInvoices = this.hasUserRole() ? invoices : invoices;
      
      this.stats.paidInvoices = filteredInvoices.filter(i => i.status === 'PAID').length;
      this.stats.pendingInvoices = filteredInvoices.filter(i => i.status === 'PENDING').length;
      this.stats.overdueInvoices = filteredInvoices.filter(i => i.status === 'OVERDUE').length;
      this.recentInvoices = filteredInvoices.slice(0, 10);
    });
  }

  getRoles(): string {
    return this.currentUser?.roles.join(', ') || '';
  }

  hasFinanceRole(): boolean {
    return this.authService.hasRole('ROLE_FINANCE') || this.authService.hasRole('ROLE_ADMIN');
  }

  hasAdminRole(): boolean {
    return this.authService.hasRole('ROLE_ADMIN');
  }

  hasManagerRole(): boolean {
    return this.authService.hasRole('ROLE_MANAGER') || this.authService.hasRole('ROLE_ADMIN');
  }

  hasUserRole(): boolean {
    return this.authService.hasRole('ROLE_USER') && !this.hasAdminRole() && !this.hasManagerRole() && !this.hasFinanceRole();
  }

  getStatusColor(status: string): string {
    const colors: any = {
      'PAID': 'success',
      'PENDING': 'warning',
      'OVERDUE': 'danger',
      'APPROVED': 'info',
      'REJECTED': 'secondary'
    };
    return colors[status] || 'secondary';
  }
}

