import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { VendorService } from '../../services/vendor.service';

@Component({
  selector: 'app-vendor-form',
  templateUrl: './vendor-form.component.html',
  styleUrls: ['./vendor-form.component.css']
})
export class VendorFormComponent implements OnInit {
  vendorForm!: FormGroup;
  isEditMode = false;
  vendorId?: number;
  errorMessage: string = '';
  isLoading: boolean = false;

  constructor(
    private fb: FormBuilder,
    private vendorService: VendorService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.vendorForm = this.fb.group({
      name: ['', Validators.required],
      gstin: ['', [Validators.pattern(/^[0-9]*$/)]],
      email: ['', [Validators.required, Validators.email]],
      status: ['ACTIVE']
    });

    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.vendorId = +params['id'];
        this.loadVendor();
      }
    });
  }

  loadVendor(): void {
    if (this.vendorId) {
      this.vendorService.getVendorById(this.vendorId).subscribe({
        next: (vendor) => this.vendorForm.patchValue(vendor),
        error: (err) => console.error('Error loading vendor:', err)
      });
    }
  }

  onSubmit(): void {
    if (this.vendorForm.valid) {
      this.errorMessage = '';
      this.isLoading = true;
      
      // Clean up the vendor data - remove empty GSTIN if not provided
      const vendor = { ...this.vendorForm.value };
      if (!vendor.gstin || vendor.gstin.trim() === '') {
        delete vendor.gstin; // Remove empty GSTIN to avoid validation issues
      }
      
      if (this.isEditMode && this.vendorId) {
        this.vendorService.updateVendor(this.vendorId, vendor).subscribe({
          next: () => {
            this.isLoading = false;
            this.router.navigate(['/vendors']);
          },
          error: (err) => {
            this.isLoading = false;
            this.handleError(err);
          }
        });
      } else {
        this.vendorService.createVendor(vendor).subscribe({
          next: () => {
            this.isLoading = false;
            this.router.navigate(['/vendors']);
          },
          error: (err) => {
            this.isLoading = false;
            this.handleError(err);
          }
        });
      }
    } else {
      // Mark all fields as touched to show validation errors
      Object.keys(this.vendorForm.controls).forEach(key => {
        this.vendorForm.get(key)?.markAsTouched();
      });
    }
  }

  private handleError(err: any): void {
    console.error('Error:', err);
    if (err.error) {
      // Handle validation errors with field errors
      if (err.error.fieldErrors) {
        const fieldErrors = err.error.fieldErrors;
        const errorMessages = Object.keys(fieldErrors).map(
          field => `${field}: ${fieldErrors[field]}`
        );
        this.errorMessage = errorMessages.join(', ');
      } else if (err.error.message) {
        this.errorMessage = err.error.message;
      } else if (err.error.error) {
        this.errorMessage = err.error.error;
      } else if (typeof err.error === 'string') {
        this.errorMessage = err.error;
      } else {
        this.errorMessage = 'An error occurred. Please try again.';
      }
    } else if (err.message) {
      this.errorMessage = err.message;
    } else {
      this.errorMessage = 'Failed to save vendor. Please check your connection and try again.';
    }
  }

  cancel(): void {
    this.router.navigate(['/vendors']);
  }

  onGstinKeyPress(event: KeyboardEvent): void {
    // Allow only numbers (0-9) and control keys (backspace, delete, tab, etc.)
    const charCode = event.which ? event.which : event.keyCode;
    if (charCode > 31 && (charCode < 48 || charCode > 57)) {
      event.preventDefault();
    }
  }
}

