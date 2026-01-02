import { Component, OnInit } from '@angular/core';
import { AuditLogService, AuditLog } from '../services/audit-log.service';

@Component({
  selector: 'app-audit-logs',
  templateUrl: './audit-logs.component.html',
  styleUrls: ['./audit-logs.component.css']
})
export class AuditLogsComponent implements OnInit {
  auditLogs: AuditLog[] = [];
  loading = false;
  error: string = '';

  constructor(private auditLogService: AuditLogService) {}

  ngOnInit(): void {
    this.loadAuditLogs();
  }

  loadAuditLogs(): void {
    this.loading = true;
    this.error = '';
    this.auditLogService.getAllAuditLogs().subscribe({
      next: (logs) => {
        this.auditLogs = logs || [];
        this.loading = false;
      },
      error: (err) => {
        if (err.status === 403) {
          this.error = 'Access denied. Only ADMIN users can view audit logs.';
        } else if (err.status === 401) {
          this.error = 'Unauthorized. Please login again.';
        } else {
          this.error = `Failed to load audit logs: ${err.error?.message || err.message || 'Unknown error'}. Check console for details.`;
        }
        this.loading = false;
      }
    });
  }

  getActionBadgeClass(action: string): string {
    switch (action?.toUpperCase()) {
      case 'CREATE':
        return 'badge bg-success';
      case 'UPDATE':
        return 'badge bg-primary';
      case 'DELETE':
        return 'badge bg-danger';
      case 'APPROVE':
        return 'badge bg-info';
      case 'REJECT':
        return 'badge bg-warning';
      default:
        return 'badge bg-secondary';
    }
  }
}

