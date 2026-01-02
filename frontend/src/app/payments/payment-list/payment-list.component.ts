import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PaymentService, Payment } from '../../services/payment.service';

@Component({
  selector: 'app-payment-list',
  templateUrl: './payment-list.component.html',
  styleUrls: ['./payment-list.component.css']
})
export class PaymentListComponent implements OnInit {
  payments: Payment[] = [];
  filteredPayments: Payment[] = [];
  
  // Search
  searchTerm: string = '';
  
  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10;
  totalPages: number = 1;
  
  // Modal
  showConfirmModal: boolean = false;
  confirmTitle: string = 'Confirm Action';
  confirmMessage: string = '';
  confirmCallback: (() => void) | null = null;

  constructor(private paymentService: PaymentService, private router: Router) {}

  ngOnInit(): void {
    this.loadPayments();
  }

  loadPayments(): void {
    this.paymentService.getAllPayments().subscribe({
      next: (data) => {
        this.payments = data;
        this.applyFilters();
      },
      error: (err) => console.error('Error loading payments:', err)
    });
  }

  applyFilters(): void {
    let filtered = this.payments;
    
    if (this.searchTerm.trim()) {
      const search = this.searchTerm.toLowerCase().trim();
      filtered = this.payments.filter(payment => 
        payment.id?.toString().includes(search) ||
        payment.invoiceId?.toString().includes(search) ||
        payment.amount?.toString().includes(search) ||
        payment.paymentMethod?.toLowerCase().includes(search) ||
        payment.transactionReference?.toLowerCase().includes(search)
      );
    }
    
    this.filteredPayments = filtered;
    this.totalPages = Math.ceil(this.filteredPayments.length / this.itemsPerPage);
    this.currentPage = 1;
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  getPaginatedPayments(): Payment[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredPayments.slice(startIndex, endIndex);
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

  deletePayment(id: number): void {
    this.confirmTitle = 'Confirm Deletion';
    this.confirmMessage = 'Are you sure you want to delete this payment?';
    this.confirmCallback = () => {
      this.paymentService.deletePayment(id).subscribe({
        next: () => this.loadPayments(),
        error: (err) => console.error('Error deleting payment:', err)
      });
    };
    this.showConfirmModal = true;
  }

  onConfirm(): void {
    if (this.confirmCallback) {
      this.confirmCallback();
      this.confirmCallback = null;
    }
    this.showConfirmModal = false;
  }

  onCancelConfirm(): void {
    this.showConfirmModal = false;
    this.confirmCallback = null;
  }
}

