import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { VendorService, Vendor } from '../../services/vendor.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-vendor-list',
  templateUrl: './vendor-list.component.html',
  styleUrls: ['./vendor-list.component.css']
})
export class VendorListComponent implements OnInit {
  vendors: Vendor[] = [];
  filteredVendors: Vendor[] = [];
  
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

  constructor(
    private vendorService: VendorService, 
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadVendors();
  }

  loadVendors(): void {
    this.vendorService.getAllVendors().subscribe({
      next: (data) => {
        this.vendors = data;
        this.applyFilters();
      },
      error: (err) => console.error('Error loading vendors:', err)
    });
  }

  applyFilters(): void {
    let filtered = this.vendors;
    
    if (this.searchTerm.trim()) {
      const search = this.searchTerm.toLowerCase().trim();
      filtered = this.vendors.filter(vendor => 
        vendor.name?.toLowerCase().includes(search) ||
        vendor.email?.toLowerCase().includes(search) ||
        vendor.gstin?.toLowerCase().includes(search) ||
        vendor.status?.toLowerCase().includes(search) ||
        vendor.riskScore?.toString().includes(search)
      );
    }
    
    this.filteredVendors = filtered;
    this.totalPages = Math.ceil(this.filteredVendors.length / this.itemsPerPage);
    this.currentPage = 1;
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  getPaginatedVendors(): Vendor[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredVendors.slice(startIndex, endIndex);
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

  editVendor(id: number): void {
    this.router.navigate(['/vendors/edit', id]);
  }

  deleteVendor(id: number): void {
    this.confirmTitle = 'Confirm Deletion';
    this.confirmMessage = 'Are you sure you want to delete this vendor?';
    this.confirmCallback = () => {
      this.vendorService.deleteVendor(id).subscribe({
        next: () => this.loadVendors(),
        error: (err) => console.error('Error deleting vendor:', err)
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

  canEditVendor(): boolean {
    return this.authService.hasRole('ROLE_ADMIN');
  }

  canDeleteVendor(): boolean {
    return this.authService.hasRole('ROLE_ADMIN');
  }
}

