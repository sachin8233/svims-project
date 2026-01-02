import { Component, OnInit } from '@angular/core';
import { InvoiceService, Invoice } from '../services/invoice.service';

@Component({
  selector: 'app-approved-invoices',
  templateUrl: './approved-invoices.component.html',
  styleUrls: ['./approved-invoices.component.css']
})
export class ApprovedInvoicesComponent implements OnInit {
  approvedInvoices: Invoice[] = [];

  constructor(private invoiceService: InvoiceService) {}

  ngOnInit(): void {
    this.loadApprovedInvoices();
  }

  loadApprovedInvoices(): void {
    this.invoiceService.getAllInvoices().subscribe({
      next: (data) => {
        this.approvedInvoices = data.filter(inv => inv.status === 'APPROVED');
      },
      error: (err) => console.error('Error loading approved invoices:', err)
    });
  }
}


