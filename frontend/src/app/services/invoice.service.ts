import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface InvoiceItem {
  id?: number;
  description: string;
  quantity: number;
  unitPrice: number;
  amount?: number;
  itemOrder?: number;
}

export interface Invoice {
  id?: number;
  vendorId: number;
  vendorName?: string;
  amount?: number;
  cgstAmount?: number;
  sgstAmount?: number;
  igstAmount?: number;
  totalAmount?: number;
  invoiceDate: string;
  dueDate: string;
  status?: string;
  invoiceNumber?: string;
  currentApprovalLevel?: number;
  isOverdue?: boolean;
  escalationLevel?: number;
  items?: InvoiceItem[];
  createdAt?: string;
  updatedAt?: string;
}

export interface InvoiceApprovalInfo {
  currentApprovalLevel: number;
  requiredApprovalLevels: number;
  remainingApprovals: number;
  isFullyApproved: boolean;
  approvalRuleRange: string;
}

@Injectable({
  providedIn: 'root'
})
export class InvoiceService {
  private apiUrl = 'http://localhost:8080/api/invoices';

  constructor(private http: HttpClient) {}

  getAllInvoices(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(this.apiUrl);
  }

  getInvoiceById(id: number): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.apiUrl}/${id}`);
  }

  createInvoice(invoice: Invoice): Observable<Invoice> {
    return this.http.post<Invoice>(this.apiUrl, invoice);
  }

  updateInvoice(id: number, invoice: Invoice): Observable<Invoice> {
    return this.http.put<Invoice>(`${this.apiUrl}/${id}`, invoice);
  }


  approveInvoice(id: number, level: number, comments?: string): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.apiUrl}/${id}/approve?level=${level}&comments=${comments || ''}`, {});
  }

  rejectInvoice(id: number, comments: string): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.apiUrl}/${id}/reject?comments=${comments}`, {});
  }

  getOverdueInvoices(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${this.apiUrl}/overdue`);
  }

  getInvoiceApprovalInfo(id: number): Observable<InvoiceApprovalInfo> {
    return this.http.get<InvoiceApprovalInfo>(`${this.apiUrl}/${id}/approval-info`);
  }
}

