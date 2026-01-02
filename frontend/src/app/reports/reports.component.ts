import { Component, OnInit } from '@angular/core';
import { ReportsService, ReportsData } from '../services/reports.service';

@Component({
  selector: 'app-reports',
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.css']
})
export class ReportsComponent implements OnInit {
  reportsData?: ReportsData;
  loading = true;
  errorMessage: string = '';

  constructor(private reportsService: ReportsService) {}

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.loading = true;
    this.reportsService.getDashboardReports().subscribe({
      next: (data) => {
        this.reportsData = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading reports:', err);
        this.errorMessage = 'Failed to load reports data';
        this.loading = false;
      }
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount);
  }

  formatNumber(num: number): string {
    return new Intl.NumberFormat('en-IN').format(num);
  }

  getStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      'PENDING': 'warning',
      'APPROVED': 'info',
      'PAID': 'success',
      'OVERDUE': 'danger',
      'REJECTED': 'secondary',
      'PARTIALLY_PAID': 'primary',
      'ESCALATED': 'danger'
    };
    return colors[status] || 'secondary';
  }

  // Helper to access Object.keys in template
  Object = Object;
}

