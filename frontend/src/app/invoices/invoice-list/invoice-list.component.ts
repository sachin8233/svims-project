import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { InvoiceService, Invoice, InvoiceApprovalInfo } from '../../services/invoice.service';
import { VendorService } from '../../services/vendor.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-invoice-list',
  templateUrl: './invoice-list.component.html',
  styleUrls: ['./invoice-list.component.css']
})
export class InvoiceListComponent implements OnInit {
  invoices: Invoice[] = [];
  filteredInvoices: Invoice[] = [];
  approvalInfoMap: Map<number, InvoiceApprovalInfo> = new Map();
  
  // Search
  searchTerm: string = '';
  
  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10;
  totalPages: number = 1;
  
  // Modal
  showModal: boolean = false;
  modalTitle: string = '';
  modalMessage: string = '';
  modalType: 'success' | 'error' | 'info' | 'warning' = 'info';
  
  // Input Modal
  showInputModal: boolean = false;
  inputModalTitle: string = '';
  inputModalMessage: string = '';
  inputModalPlaceholder: string = '';
  inputModalCallback: ((value: string | null) => void) | null = null;

  constructor(
    private invoiceService: InvoiceService,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadInvoices();
  }

  loadInvoices(): void {
    this.invoiceService.getAllInvoices().subscribe({
      next: (data) => {
        this.invoices = data;
        this.applyFilters();
        this.loadApprovalInfoForPendingInvoices();
      },
      error: (err) => console.error('Error loading invoices:', err)
    });
  }

  applyFilters(): void {
    // Apply search filter
    let filtered = this.invoices;
    
    if (this.searchTerm.trim()) {
      const search = this.searchTerm.toLowerCase().trim();
      filtered = this.invoices.filter(invoice => 
        invoice.invoiceNumber?.toLowerCase().includes(search) ||
        invoice.vendorName?.toLowerCase().includes(search) ||
        invoice.status?.toLowerCase().includes(search) ||
        invoice.amount?.toString().includes(search) ||
        invoice.totalAmount?.toString().includes(search)
      );
    }
    
    this.filteredInvoices = filtered;
    this.totalPages = Math.ceil(this.filteredInvoices.length / this.itemsPerPage);
    this.currentPage = 1; // Reset to first page when filtering
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  getPaginatedInvoices(): Invoice[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredInvoices.slice(startIndex, endIndex);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPages = 5; // Show max 5 page numbers
    let startPage = Math.max(1, this.currentPage - Math.floor(maxPages / 2));
    let endPage = Math.min(this.totalPages, startPage + maxPages - 1);
    
    if (endPage - startPage < maxPages - 1) {
      startPage = Math.max(1, endPage - maxPages + 1);
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  }

  loadApprovalInfoForPendingInvoices(): void {
    this.approvalInfoMap.clear();
    const pendingInvoices = this.invoices.filter(inv => inv.status === 'PENDING' && inv.id);
    
    pendingInvoices.forEach(invoice => {
      if (invoice.id) {
        this.invoiceService.getInvoiceApprovalInfo(invoice.id).subscribe({
          next: (info) => {
            this.approvalInfoMap.set(invoice.id!, info);
          },
          error: (err) => console.error(`Error loading approval info for invoice ${invoice.id}:`, err)
        });
      }
    });
  }

  getApprovalInfo(invoiceId: number): InvoiceApprovalInfo | undefined {
    return this.approvalInfoMap.get(invoiceId);
  }

  editInvoice(id: number): void {
    this.router.navigate(['/invoices/edit', id]);
  }

  viewInvoice(id: number): void {
    this.router.navigate(['/invoices/view', id]);
  }

  approveInvoice(invoice: Invoice): void {
    const currentLevel = invoice.currentApprovalLevel || 0;
    const nextLevel = currentLevel + 1;
    
    this.inputModalTitle = 'Approval Comments';
    this.inputModalMessage = `Enter approval comments for Level ${nextLevel} (optional):`;
    this.inputModalPlaceholder = 'Enter comments...';
    this.inputModalCallback = (comments: string | null) => {
      this.invoiceService.approveInvoice(invoice.id!, nextLevel, comments || `Approved at level ${nextLevel}`).subscribe({
        next: (updatedInvoice) => {
          // Check if invoice is still pending (means it was already approved or needs more approvals)
          if (updatedInvoice.status === 'PENDING') {
            this.showModalMessage('Info', 'This invoice level has already been approved or requires additional approvals. The invoice remains pending.', 'info');
          } else {
            this.showModalMessage('Success', 'Invoice approved successfully!', 'success');
          }
          this.loadInvoices();
        },
        error: (err) => {
          console.error('Error approving invoice:', err);
          const errorMsg = err.error?.error || err.message || 'Failed to approve invoice';
          this.showModalMessage('Error', errorMsg, 'error');
        }
      });
    };
    this.showInputModal = true;
  }

  rejectInvoice(id: number): void {
    this.inputModalTitle = 'Rejection Reason';
    this.inputModalMessage = 'Enter rejection reason:';
    this.inputModalPlaceholder = 'Enter rejection reason...';
    this.inputModalCallback = (comments: string | null) => {
      if (comments) {
        this.invoiceService.rejectInvoice(id, comments).subscribe({
          next: () => this.loadInvoices(),
          error: (err) => console.error('Error rejecting invoice:', err)
        });
      }
    };
    this.showInputModal = true;
  }

  getStatusColor(status: string | undefined): string {
    if (!status) {
      return 'secondary';
    }
    const colors: any = {
      'PAID': 'success',
      'PENDING': 'warning',
      'OVERDUE': 'danger',
      'APPROVED': 'info',
      'REJECTED': 'secondary'
    };
    return colors[status] || 'secondary';
  }

  showModalMessage(title: string, message: string, type: 'success' | 'error' | 'info' | 'warning' = 'info'): void {
    this.modalTitle = title;
    this.modalMessage = message;
    this.modalType = type;
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  getTotalGst(invoice: Invoice): number {
    const cgst = invoice.cgstAmount || 0;
    const sgst = invoice.sgstAmount || 0;
    const igst = invoice.igstAmount || 0;
    return cgst + sgst + igst;
  }

  canEditInvoice(status: string | undefined): boolean {
    if (!status) {
      return false;
    }
    // Only allow editing if invoice is PENDING, REJECTED, or CREATED
    // Don't allow editing if invoice is APPROVED, PAID, or PARTIALLY_PAID
    const editableStatuses = ['PENDING', 'REJECTED', 'CREATED', 'OVERDUE', 'ESCALATED'];
    return editableStatuses.includes(status);
  }

  canCreateInvoice(): boolean {
    // Only USER and ADMIN can create invoices
    return this.authService.hasRole('ROLE_USER') || this.authService.hasRole('ROLE_ADMIN');
  }

  onInputConfirm(value: string | null): void {
    if (this.inputModalCallback) {
      this.inputModalCallback(value);
    }
    this.showInputModal = false;
    // Reset callback
    this.inputModalCallback = null;
  }

  onInputCancel(): void {
    this.showInputModal = false;
    // Reset callback
    this.inputModalCallback = null;
  }

  // Helper method for template
  Math = Math;
}

