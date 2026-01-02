import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { PaymentService } from '../../services/payment.service';
import { InvoiceService } from '../../services/invoice.service';

@Component({
  selector: 'app-payment-form',
  templateUrl: './payment-form.component.html',
  styleUrls: ['./payment-form.component.css']
})
export class PaymentFormComponent implements OnInit {
  paymentForm!: FormGroup;
  invoices: any[] = [];

  constructor(
    private fb: FormBuilder,
    private paymentService: PaymentService,
    private invoiceService: InvoiceService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.paymentForm = this.fb.group({
      invoiceId: ['', Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      paymentDate: [new Date().toISOString().slice(0, 16)],
      paymentMethod: ['BANK_TRANSFER'],
      transactionReference: [''],
      notes: ['']
    });

    this.loadInvoices();
  }

  loadInvoices(): void {
    this.invoiceService.getAllInvoices().subscribe({
      next: (data) => this.invoices = data.filter(i => 
        i.status !== 'PAID' && i.status !== 'REJECTED'
      ),
      error: (err) => console.error('Error loading invoices:', err)
    });
  }

  onSubmit(): void {
    if (this.paymentForm.valid) {
      const payment = this.paymentForm.value;
      this.paymentService.createPayment(payment).subscribe({
        next: () => this.router.navigate(['/payments']),
        error: (err) => console.error('Error creating payment:', err)
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/payments']);
  }
}

