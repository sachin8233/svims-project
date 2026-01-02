import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { InvoiceService, Invoice, InvoiceApprovalInfo } from '../../services/invoice.service';

@Component({
  selector: 'app-invoice-view',
  templateUrl: './invoice-view.component.html',
  styleUrls: ['./invoice-view.component.css']
})
export class InvoiceViewComponent implements OnInit {
  invoice: Invoice | null = null;
  approvalInfo: InvoiceApprovalInfo | null = null;
  loading = true;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private invoiceService: InvoiceService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadInvoice(+id);
    } else {
      this.error = 'Invoice ID not provided';
      this.loading = false;
    }
  }

  loadInvoice(id: number): void {
    this.loading = true;
    this.invoiceService.getInvoiceById(id).subscribe({
      next: (invoice) => {
        this.invoice = invoice;
        this.loading = false;
        
        // Load approval info if invoice is pending
        if (invoice.status === 'PENDING' && invoice.id) {
          this.loadApprovalInfo(invoice.id);
        }
      },
      error: (err) => {
        console.error('Error loading invoice:', err);
        this.error = 'Failed to load invoice: ' + (err.error?.error || err.message);
        this.loading = false;
      }
    });
  }

  loadApprovalInfo(invoiceId: number): void {
    this.invoiceService.getInvoiceApprovalInfo(invoiceId).subscribe({
      next: (info) => {
        this.approvalInfo = info;
      },
      error: (err) => {
        console.error('Error loading approval info:', err);
      }
    });
  }

  getTotalGst(): number {
    if (!this.invoice) return 0;
    const cgst = this.invoice.cgstAmount || 0;
    const sgst = this.invoice.sgstAmount || 0;
    const igst = this.invoice.igstAmount || 0;
    return cgst + sgst + igst;
  }

  getStatusColor(status: string | undefined): string {
    if (!status) return 'secondary';
    const colors: any = {
      'PAID': 'success',
      'PENDING': 'warning',
      'OVERDUE': 'danger',
      'APPROVED': 'info',
      'REJECTED': 'secondary',
      'PARTIALLY_PAID': 'primary',
      'ESCALATED': 'danger'
    };
    return colors[status] || 'secondary';
  }

  formatCurrency(amount: number | undefined): string {
    if (amount === undefined || amount === null) {
      return '₹0.00';
    }
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(amount);
  }

  formatDate(date: string | undefined): string {
    if (!date) return '-';
    try {
      const dateObj = new Date(date);
      if (isNaN(dateObj.getTime())) return date; // Return original if invalid
      return dateObj.toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch (e) {
      return date; // Return original string if parsing fails
    }
  }

  goBack(): void {
    this.router.navigate(['/invoices']);
  }

  editInvoice(): void {
    if (this.invoice?.id) {
      this.router.navigate(['/invoices/edit', this.invoice.id]);
    }
  }

  canEdit(): boolean {
    if (!this.invoice?.status) return false;
    // Only allow editing if invoice is PENDING, REJECTED, or CREATED
    // Don't allow editing if invoice is APPROVED, PAID, or PARTIALLY_PAID
    const editableStatuses = ['PENDING', 'REJECTED', 'CREATED', 'OVERDUE', 'ESCALATED'];
    return editableStatuses.includes(this.invoice.status);
  }
}

