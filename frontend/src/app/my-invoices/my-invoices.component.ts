import { Component, OnInit } from '@angular/core';
import { InvoiceService, Invoice } from '../services/invoice.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-my-invoices',
  templateUrl: './my-invoices.component.html',
  styleUrls: ['./my-invoices.component.css']
})
export class MyInvoicesComponent implements OnInit {
  invoices: Invoice[] = [];
  currentUser: any;

  constructor(
    private invoiceService: InvoiceService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadMyInvoices();
  }

  loadMyInvoices(): void {
    // For now, load all invoices. Later can filter by user if needed
    this.invoiceService.getAllInvoices().subscribe({
      next: (data) => {
        this.invoices = data;
      },
      error: (err) => console.error('Error loading invoices:', err)
    });
  }

  getStatusColor(status: string | undefined): string {
    if (!status) return 'secondary';
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

