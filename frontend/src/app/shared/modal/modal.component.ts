import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
  styleUrls: ['./modal.component.css']
})
export class ModalComponent {
  @Input() title: string = '';
  @Input() message: string = '';
  @Input() type: 'success' | 'error' | 'info' | 'warning' = 'info';
  @Input() showModal: boolean = false;
  @Output() closeModal = new EventEmitter<void>();

  close(): void {
    this.showModal = false;
    this.closeModal.emit();
  }

  getIconClass(): string {
    switch (this.type) {
      case 'success':
        return 'bi bi-check-circle-fill text-success';
      case 'error':
        return 'bi bi-x-circle-fill text-danger';
      case 'warning':
        return 'bi bi-exclamation-triangle-fill text-warning';
      default:
        return 'bi bi-info-circle-fill text-info';
    }
  }

  getModalClass(): string {
    switch (this.type) {
      case 'success':
        return 'border-success';
      case 'error':
        return 'border-danger';
      case 'warning':
        return 'border-warning';
      default:
        return 'border-info';
    }
  }
}

