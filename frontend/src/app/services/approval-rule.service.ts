import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApprovalRule {
  id?: number;
  minAmount: number;
  maxAmount: number;
  approvalLevels: number;
  requiredRoles?: string;
  isActive?: boolean;
  priority?: number;
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApprovalRuleService {
  private apiUrl = 'http://localhost:8080/api/approval-rules';

  constructor(private http: HttpClient) {}

  getAllApprovalRules(): Observable<ApprovalRule[]> {
    return this.http.get<ApprovalRule[]>(this.apiUrl);
  }

  getActiveApprovalRules(): Observable<ApprovalRule[]> {
    return this.http.get<ApprovalRule[]>(`${this.apiUrl}/active`);
  }

  getApprovalRuleById(id: number): Observable<ApprovalRule> {
    return this.http.get<ApprovalRule>(`${this.apiUrl}/${id}`);
  }

  createApprovalRule(rule: ApprovalRule): Observable<ApprovalRule> {
    return this.http.post<ApprovalRule>(this.apiUrl, rule);
  }

  updateApprovalRule(id: number, rule: ApprovalRule): Observable<ApprovalRule> {
    return this.http.put<ApprovalRule>(`${this.apiUrl}/${id}`, rule);
  }

  deleteApprovalRule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  toggleApprovalRuleStatus(id: number): Observable<ApprovalRule> {
    return this.http.put<ApprovalRule>(`${this.apiUrl}/${id}/toggle-status`, {});
  }
}


