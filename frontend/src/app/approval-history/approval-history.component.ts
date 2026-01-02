import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-approval-history',
  templateUrl: './approval-history.component.html',
  styleUrls: ['./approval-history.component.css']
})
export class ApprovalHistoryComponent implements OnInit {
  approvalHistory: any[] = [];

  constructor() {}

  ngOnInit(): void {
    // TODO: Load approval history from API
    this.approvalHistory = [];
  }
}

