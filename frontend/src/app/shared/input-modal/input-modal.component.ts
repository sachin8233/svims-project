import { Component, Input, Output, EventEmitter, ViewChild, ElementRef } from '@angular/core';

@Component({
  selector: 'app-input-modal',
  templateUrl: './input-modal.component.html',
  styleUrls: ['./input-modal.component.css']
})
export class InputModalComponent {
  @Input() title: string = 'Input Required';
  @Input() message: string = 'Please enter a value:';
  @Input() placeholder: string = '';
  @Input() showModal: boolean = false;
  @Input() required: boolean = false;
  @Output() confirm = new EventEmitter<string | null>();
  @Output() cancel = new EventEmitter<void>();
  @ViewChild('inputField') inputField!: ElementRef<HTMLInputElement>;
  
  inputValue: string = '';

  ngOnChanges(): void {
    if (this.showModal && this.inputField) {
      // Focus input when modal opens
      setTimeout(() => {
        this.inputField?.nativeElement?.focus();
      }, 100);
    }
  }

  onConfirm(): void {
    if (this.required && !this.inputValue.trim()) {
      return; // Don't close if required and empty
    }
    this.showModal = false;
    this.confirm.emit(this.inputValue.trim() || null);
    this.inputValue = ''; // Reset for next time
  }

  onCancel(): void {
    this.showModal = false;
    this.cancel.emit();
    this.inputValue = ''; // Reset for next time
  }

  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.onConfirm();
    } else if (event.key === 'Escape') {
      this.onCancel();
    }
  }
}

