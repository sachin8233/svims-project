import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface InvoiceStats {
  totalInvoices: number;
  pendingInvoices: number;
  approvedInvoices: number;
  paidInvoices: number;
  overdueInvoices: number;
  rejectedInvoices: number;
  totalInvoiceAmount: number;
  pendingAmount: number;
  approvedAmount: number;
  paidAmount: number;
  overdueAmount: number;
}

export interface FinancialSummary {
  totalInvoiceAmount: number;
  totalPaidAmount: number;
  totalOutstandingAmount: number;
  totalCgstAmount: number;
  totalSgstAmount: number;
  totalIgstAmount: number;
  totalGstAmount: number;
}

export interface VendorStats {
  totalVendors: number;
  activeVendors: number;
  highRiskVendors: number;
  averageRiskScore: number;
}

export interface MonthlyData {
  month: string;
  invoiceCount: number;
  invoiceAmount: number;
  paidAmount: number;
}

export interface VendorSummary {
  vendorId: number;
  vendorName: string;
  invoiceCount: number;
  totalAmount: number;
  paidAmount: number;
  outstandingAmount: number;
  riskScore?: number;
}

export interface ReportsData {
  invoiceStats: InvoiceStats;
  financialSummary: FinancialSummary;
  vendorStats: VendorStats;
  statusBreakdown: { [key: string]: number };
  monthlyTrends: MonthlyData[];
  topVendors: VendorSummary[];
}

@Injectable({
  providedIn: 'root'
})
export class ReportsService {
  private apiUrl = 'http://localhost:8080/api/reports';

  constructor(private http: HttpClient) {}

  getDashboardReports(): Observable<ReportsData> {
    return this.http.get<ReportsData>(`${this.apiUrl}/dashboard`);
  }
}


