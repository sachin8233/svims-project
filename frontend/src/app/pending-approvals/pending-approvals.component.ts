import { Component, OnInit } from '@angular/core';
import { InvoiceService, Invoice, InvoiceApprovalInfo } from '../services/invoice.service';

@Component({
  selector: 'app-pending-approvals',
  templateUrl: './pending-approvals.component.html',
  styleUrls: ['./pending-approvals.component.css']
})
export class PendingApprovalsComponent implements OnInit {
  pendingInvoices: Invoice[] = [];
  filteredInvoices: Invoice[] = [];
  approvalInfoMap: Map<number, InvoiceApprovalInfo> = new Map();
  loading = false;
  
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
  
  // Search
  searchTerm: string = '';
  
  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10;
  totalPages: number = 1;

  constructor(private invoiceService: InvoiceService) {}

  ngOnInit(): void {
    this.loadPendingInvoices();
  }

  loadPendingInvoices(): void {
    this.loading = true;
    this.invoiceService.getAllInvoices().subscribe({
      next: (data) => {
        this.pendingInvoices = data.filter(inv => inv.status === 'PENDING');
        this.applyFilters();
        this.loadApprovalInfoForInvoices();
      },
      error: (err) => {
        console.error('Error loading pending invoices:', err);
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = this.pendingInvoices;
    
    if (this.searchTerm.trim()) {
      const search = this.searchTerm.toLowerCase().trim();
      filtered = this.pendingInvoices.filter(invoice => 
        invoice.invoiceNumber?.toLowerCase().includes(search) ||
        invoice.vendorName?.toLowerCase().includes(search) ||
        invoice.amount?.toString().includes(search) ||
        invoice.totalAmount?.toString().includes(search)
      );
    }
    
    this.filteredInvoices = filtered;
    this.totalPages = Math.ceil(this.filteredInvoices.length / this.itemsPerPage);
    this.currentPage = 1;
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
    const maxPages = 5;
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

  Math = Math;

  loadApprovalInfoForInvoices(): void {
    this.approvalInfoMap.clear();
    let loaded = 0;
    const total = this.pendingInvoices.length;

    if (total === 0) {
      this.loading = false;
      return;
    }

    this.pendingInvoices.forEach(invoice => {
      if (invoice.id) {
        this.invoiceService.getInvoiceApprovalInfo(invoice.id).subscribe({
          next: (info) => {
            this.approvalInfoMap.set(invoice.id!, info);
            loaded++;
            if (loaded === total) {
              this.loading = false;
            }
          },
          error: (err) => {
            console.error(`Error loading approval info for invoice ${invoice.id}:`, err);
            loaded++;
            if (loaded === total) {
              this.loading = false;
            }
          }
        });
      }
    });
  }

  getApprovalInfo(invoiceId: number): InvoiceApprovalInfo | undefined {
    return this.approvalInfoMap.get(invoiceId);
  }

  approveInvoice(invoice: Invoice): void {
    const approvalInfo = this.getApprovalInfo(invoice.id!);
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
        this.loadPendingInvoices();
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

  onInputConfirm(value: string | null): void {
    if (this.inputModalCallback) {
      this.inputModalCallback(value);
      this.inputModalCallback = null;
    }
    this.showInputModal = false;
  }

  onInputCancel(): void {
    this.showInputModal = false;
    this.inputModalCallback = null;
  }

  rejectInvoice(id: number): void {
    this.inputModalTitle = 'Rejection Reason';
    this.inputModalMessage = 'Enter rejection reason:';
    this.inputModalPlaceholder = 'Enter rejection reason...';
    this.inputModalCallback = (comments: string | null) => {
      if (comments) {
        this.invoiceService.rejectInvoice(id, comments).subscribe({
          next: () => this.loadPendingInvoices(),
          error: (err) => {
            console.error('Error rejecting invoice:', err);
            const errorMsg = err.error?.error || err.message || 'Failed to reject invoice';
            this.showModalMessage('Error', errorMsg, 'error');
          }
        });
      }
    };
    this.showInputModal = true;
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
}

