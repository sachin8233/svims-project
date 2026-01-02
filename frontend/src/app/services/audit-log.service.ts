import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AuditLog {
  id: number;
  userName: string;
  action: string;
  entityType: string;
  entityId: number;
  oldValue: string;
  newValue: string;
  ipAddress: string;
  timestamp: string;
  description: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {
  private apiUrl = 'http://localhost:8080/api/audit-logs';

  constructor(private http: HttpClient) {}

  getAllAuditLogs(): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.apiUrl}/all`);
  }

  getAuditLogsByUser(userName: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.apiUrl}/user/${userName}`);
  }

  getAuditLogsByEntity(entityType: string, entityId: number): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.apiUrl}/entity/${entityType}/${entityId}`);
  }

  getAuditLogsByDateRange(startDate: string, endDate: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.apiUrl}/date-range`, {
      params: { startDate, endDate }
    });
  }
}

