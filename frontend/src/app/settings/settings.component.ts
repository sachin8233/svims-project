import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ApprovalRuleService, ApprovalRule } from '../services/approval-rule.service';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent implements OnInit {
  approvalRules: ApprovalRule[] = [];
  ruleForm!: FormGroup;
  isEditMode = false;
  editingRuleId?: number;
  showForm = false;
  errorMessage: string = '';
  successMessage: string = '';
  
  // Modal
  showConfirmModal: boolean = false;
  confirmTitle: string = 'Confirm Action';
  confirmMessage: string = '';
  confirmCallback: (() => void) | null = null;

  constructor(
    private approvalRuleService: ApprovalRuleService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.loadApprovalRules();
  }

  initializeForm(): void {
    this.ruleForm = this.fb.group({
      minAmount: ['', [Validators.required, Validators.min(0)]],
      maxAmount: ['', [Validators.required, Validators.min(0.01)]],
      approvalLevels: ['', [Validators.required, Validators.min(1), Validators.max(4)]],
      requiredRoles: [''],
      priority: [0, [Validators.min(0)]],
      isActive: [true]
    });
  }

  loadApprovalRules(): void {
    this.approvalRuleService.getAllApprovalRules().subscribe({
      next: (rules) => {
        this.approvalRules = rules;
      },
      error: (err) => {
        console.error('Error loading approval rules:', err);
        this.errorMessage = 'Failed to load approval rules';
      }
    });
  }

  openAddForm(): void {
    this.isEditMode = false;
    this.editingRuleId = undefined;
    this.showForm = true;
    this.ruleForm.reset({
      minAmount: '',
      maxAmount: '',
      approvalLevels: '',
      requiredRoles: '',
      priority: 0,
      isActive: true
    });
    this.errorMessage = '';
    this.successMessage = '';
  }

  openEditForm(rule: ApprovalRule): void {
    this.isEditMode = true;
    this.editingRuleId = rule.id;
    this.showForm = true;
    this.ruleForm.patchValue({
      minAmount: rule.minAmount,
      maxAmount: rule.maxAmount,
      approvalLevels: rule.approvalLevels,
      requiredRoles: rule.requiredRoles || '',
      priority: rule.priority || 0,
      isActive: rule.isActive !== undefined ? rule.isActive : true
    });
    this.errorMessage = '';
    this.successMessage = '';
  }

  cancelForm(): void {
    this.showForm = false;
    this.isEditMode = false;
    this.editingRuleId = undefined;
    this.ruleForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
  }

  onSubmit(): void {
    if (this.ruleForm.valid) {
      const ruleData = this.ruleForm.value;
      
      // Validate min < max
      if (ruleData.minAmount >= ruleData.maxAmount) {
        this.errorMessage = 'Minimum amount must be less than maximum amount';
        return;
      }

      if (this.isEditMode && this.editingRuleId) {
        this.approvalRuleService.updateApprovalRule(this.editingRuleId, ruleData).subscribe({
          next: () => {
            this.successMessage = 'Approval rule updated successfully';
            this.loadApprovalRules();
            setTimeout(() => this.cancelForm(), 1500);
          },
          error: (err) => {
            this.errorMessage = err.error?.error || 'Failed to update approval rule';
          }
        });
      } else {
        this.approvalRuleService.createApprovalRule(ruleData).subscribe({
          next: () => {
            this.successMessage = 'Approval rule created successfully';
            this.loadApprovalRules();
            setTimeout(() => this.cancelForm(), 1500);
          },
          error: (err) => {
            this.errorMessage = err.error?.error || 'Failed to create approval rule';
          }
        });
      }
    }
  }

  deleteRule(id: number): void {
    this.confirmTitle = 'Confirm Deletion';
    this.confirmMessage = 'Are you sure you want to delete this approval rule?';
    this.confirmCallback = () => {
      this.approvalRuleService.deleteApprovalRule(id).subscribe({
        next: () => {
          this.successMessage = 'Approval rule deleted successfully';
          this.loadApprovalRules();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          this.errorMessage = err.error?.error || 'Failed to delete approval rule';
        }
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

  toggleRuleStatus(id: number): void {
    this.approvalRuleService.toggleApprovalRuleStatus(id).subscribe({
      next: () => {
        this.successMessage = 'Approval rule status updated';
        this.loadApprovalRules();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Failed to update approval rule status';
      }
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(amount);
  }
}

