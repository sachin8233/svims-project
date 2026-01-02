import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { InvoiceService, InvoiceItem } from '../../services/invoice.service';
import { VendorService } from '../../services/vendor.service';
// @ts-ignore
import * as XLSX from 'xlsx';

@Component({
  selector: 'app-invoice-form',
  templateUrl: './invoice-form.component.html',
  styleUrls: ['./invoice-form.component.css']
})
export class InvoiceFormComponent implements OnInit {
  invoiceForm!: FormGroup;
  isEditMode = false;
  invoiceId?: number;
  vendors: any[] = [];
  uploadError: string | null = null;
  uploadSuccess: boolean = false;
  
  // Modal
  showModal: boolean = false;
  modalTitle: string = '';
  modalMessage: string = '';
  modalType: 'success' | 'error' | 'info' | 'warning' = 'info';
  showConfirmModal: boolean = false;
  confirmCallback: (() => void) | null = null;

  constructor(
    private fb: FormBuilder,
    private invoiceService: InvoiceService,
    private vendorService: VendorService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.invoiceForm = this.fb.group({
      vendorId: ['', Validators.required],
      invoiceDate: ['', Validators.required],
      dueDate: ['', Validators.required],
      items: this.fb.array([], Validators.required)
    });

    // Add at least one item
    this.addItem();

    this.loadVendors();

    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.invoiceId = +params['id'];
        this.loadInvoice();
      }
    });
  }

  get items(): FormArray {
    return this.invoiceForm.get('items') as FormArray;
  }

  createItemFormGroup(): FormGroup {
    return this.fb.group({
      description: ['', [Validators.required]],
      quantity: [1, [Validators.required, Validators.min(1)]],
      unitPrice: ['', [Validators.required, Validators.min(0.01)]]
    });
  }

  addItem(): void {
    this.items.push(this.createItemFormGroup());
  }

  removeItem(index: number): void {
    if (this.items.length > 1) {
      this.items.removeAt(index);
    } else {
      this.showModalMessage('Warning', 'Invoice must have at least one item', 'warning');
    }
  }

  calculateItemAmount(item: any): number {
    const formGroup = item as FormGroup;
    const quantity = formGroup.get('quantity')?.value || 0;
    const unitPrice = formGroup.get('unitPrice')?.value || 0;
    return quantity * unitPrice;
  }

  calculateTotalAmount(): number {
    return this.items.controls.reduce((total, item) => {
      return total + this.calculateItemAmount(item as FormGroup);
    }, 0);
  }

  loadVendors(): void {
    this.vendorService.getAllVendors().subscribe({
      next: (data) => this.vendors = data,
      error: (err) => console.error('Error loading vendors:', err)
    });
  }

  loadInvoice(): void {
    if (this.invoiceId) {
      this.invoiceService.getInvoiceById(this.invoiceId).subscribe({
        next: (invoice) => {
          this.invoiceForm.patchValue({
            vendorId: invoice.vendorId,
            invoiceDate: invoice.invoiceDate,
            dueDate: invoice.dueDate
          });

          // Clear existing items and load invoice items
          while (this.items.length !== 0) {
            this.items.removeAt(0);
          }

          if (invoice.items && invoice.items.length > 0) {
            invoice.items.forEach(item => {
              const itemGroup = this.fb.group({
                id: [item.id],
                description: [item.description, Validators.required],
                quantity: [item.quantity, [Validators.required, Validators.min(1)]],
                unitPrice: [item.unitPrice, [Validators.required, Validators.min(0.01)]]
              });
              this.items.push(itemGroup);
            });
          } else {
            // If no items, add one empty item
            this.addItem();
          }
        },
        error: (err) => console.error('Error loading invoice:', err)
      });
    }
  }

  onSubmit(): void {
    if (this.invoiceForm.valid && this.items.length > 0) {
      const formValue = this.invoiceForm.value;
      
      // Prepare items with calculated amounts
      const items: InvoiceItem[] = formValue.items.map((item: any, index: number) => ({
        description: item.description,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        amount: item.quantity * item.unitPrice,
        itemOrder: index + 1
      }));

      const invoice = {
        vendorId: formValue.vendorId,
        invoiceDate: formValue.invoiceDate,
        dueDate: formValue.dueDate,
        items: items,
        amount: this.calculateTotalAmount() // For backward compatibility
      };

      if (this.isEditMode && this.invoiceId) {
        this.invoiceService.updateInvoice(this.invoiceId, invoice).subscribe({
          next: () => this.router.navigate(['/invoices']),
          error: (err) => {
            console.error('Error updating invoice:', err);
            const errorMsg = err.error?.error || err.message || 'Failed to update invoice';
            this.showModalMessage('Error', errorMsg, 'error');
          }
        });
      } else {
        this.invoiceService.createInvoice(invoice).subscribe({
          next: () => this.router.navigate(['/invoices']),
          error: (err) => {
            console.error('Error creating invoice:', err);
            const errorMsg = err.error?.error || err.message || 'Failed to create invoice';
            this.showModalMessage('Error', errorMsg, 'error');
          }
        });
      }
    } else {
      this.showModalMessage('Warning', 'Please fill all required fields and add at least one item', 'warning');
    }
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(amount);
  }

  cancel(): void {
    this.router.navigate(['/invoices']);
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (!file) return;

    // Reset messages
    this.uploadError = null;
    this.uploadSuccess = false;

    // Validate file type
    const validExtensions = ['.xlsx', '.xls', '.csv'];
    const fileExtension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
    
    if (!validExtensions.includes(fileExtension)) {
      this.uploadError = 'Invalid file type. Please upload Excel (.xlsx, .xls) or CSV (.csv) file.';
      return;
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      this.uploadError = 'File size exceeds 5MB limit.';
      return;
    }

    const reader = new FileReader();
    
    reader.onload = (e: any) => {
      try {
        let workbook: any;
        
        if (fileExtension === '.csv') {
          // Parse CSV
          const text = e.target.result;
          workbook = XLSX.read(text, { type: 'string' });
        } else {
          // Parse Excel
          const data = new Uint8Array(e.target.result);
          workbook = XLSX.read(data, { type: 'array' });
        }
        
        // Get first sheet
        const firstSheetName = workbook.SheetNames[0];
        const worksheet = workbook.Sheets[firstSheetName];
        
        // Convert to JSON
        const jsonData = XLSX.utils.sheet_to_json(worksheet, { header: 1, defval: '' });
        
        // Parse and populate form
        this.parseAndPopulateForm(jsonData);
        
        this.uploadSuccess = true;
        setTimeout(() => {
          this.uploadSuccess = false;
        }, 3000);
      } catch (error) {
        console.error('Error parsing file:', error);
        this.uploadError = 'Error parsing file. Please check the file format.';
      }
    };

    reader.onerror = () => {
      this.uploadError = 'Error reading file. Please try again.';
    };

    if (fileExtension === '.csv') {
      reader.readAsText(file);
    } else {
      reader.readAsArrayBuffer(file);
    }
  }

  parseAndPopulateForm(data: any[]): void {
    try {
      // Expected format:
      // Row 1: Headers (optional)
      // Row 2: Invoice Info (Vendor Name, Invoice Date, Due Date)
      // Row 3+: Items (Description, Quantity, Unit Price)

      let currentRow = 0;
      let vendorName = '';
      let invoiceDate = '';
      let dueDate = '';
      const items: any[] = [];

      // Skip empty rows at the start
      while (currentRow < data.length && (!data[currentRow] || data[currentRow].length === 0)) {
        currentRow++;
      }

      // Try to find header row (optional)
      if (currentRow < data.length) {
        const firstRow = data[currentRow];
        if (Array.isArray(firstRow) && firstRow.length > 0) {
          const firstCell = String(firstRow[0] || '').toLowerCase();
          if (firstCell.includes('vendor') || firstCell.includes('invoice') || firstCell.includes('date')) {
            currentRow++; // Skip header row
          }
        }
      }

      // Parse invoice info row
      if (currentRow < data.length) {
        const infoRow = data[currentRow];
        if (Array.isArray(infoRow)) {
          // Format 1: [Vendor Name, Invoice Date, Due Date]
          if (infoRow.length >= 3) {
            vendorName = String(infoRow[0] || '').trim();
            invoiceDate = this.parseDate(String(infoRow[1] || ''));
            dueDate = this.parseDate(String(infoRow[2] || ''));
          }
          // Format 2: Headers in first row, values in second row
          else if (currentRow + 1 < data.length) {
            const headerRow = infoRow;
            const valueRow = data[currentRow + 1];
            if (Array.isArray(headerRow) && Array.isArray(valueRow)) {
              for (let i = 0; i < headerRow.length; i++) {
                const header = String(headerRow[i] || '').toLowerCase();
                const value = String(valueRow[i] || '').trim();
                if (header.includes('vendor')) {
                  vendorName = value;
                } else if (header.includes('invoice date')) {
                  invoiceDate = this.parseDate(value);
                } else if (header.includes('due date')) {
                  dueDate = this.parseDate(value);
                }
              }
              currentRow += 2; // Skip both header and value rows
            }
          }
          currentRow++;
        }
      }

      // Skip items header row if present
      if (currentRow < data.length) {
        const itemsHeaderRow = data[currentRow];
        if (Array.isArray(itemsHeaderRow) && itemsHeaderRow.length > 0) {
          const firstCell = String(itemsHeaderRow[0] || '').toLowerCase();
          if (firstCell.includes('description') || firstCell.includes('item') || firstCell.includes('product')) {
            currentRow++; // Skip header row
          }
        }
      }

      // Parse items
      while (currentRow < data.length) {
        const row = data[currentRow];
        if (Array.isArray(row) && row.length >= 3) {
          const description = String(row[0] || '').trim();
          const quantity = this.parseNumber(row[1]);
          const unitPrice = this.parseNumber(row[2]);

          if (description && quantity > 0 && unitPrice > 0) {
            items.push({
              description: description,
              quantity: quantity,
              unitPrice: unitPrice
            });
          }
        }
        currentRow++;
      }

      // Populate form
      if (vendorName) {
        const vendor = this.vendors.find(v => 
          v.name.toLowerCase().includes(vendorName.toLowerCase()) ||
          vendorName.toLowerCase().includes(v.name.toLowerCase())
        );
        if (vendor) {
          this.invoiceForm.patchValue({ vendorId: vendor.id });
        } else {
          this.uploadError = `Vendor "${vendorName}" not found. Please select vendor manually.`;
        }
      }

      if (invoiceDate) {
        this.invoiceForm.patchValue({ invoiceDate: invoiceDate });
      }

      if (dueDate) {
        this.invoiceForm.patchValue({ dueDate: dueDate });
      }

      // Populate items
      if (items.length > 0) {
        // Clear existing items
        while (this.items.length > 0) {
          this.items.removeAt(0);
        }

        // Add parsed items
        items.forEach(item => {
          const itemGroup = this.fb.group({
            description: [item.description, Validators.required],
            quantity: [item.quantity, [Validators.required, Validators.min(1)]],
            unitPrice: [item.unitPrice, [Validators.required, Validators.min(0.01)]]
          });
          this.items.push(itemGroup);
        });
      } else {
        this.uploadError = 'No valid items found in the file.';
      }

    } catch (error) {
      console.error('Error parsing data:', error);
      this.uploadError = 'Error parsing file data. Please check the file format.';
    }
  }

  parseDate(dateStr: string): string {
    if (!dateStr) return '';
    
    // Try to parse various date formats
    const date = new Date(dateStr);
    if (!isNaN(date.getTime())) {
      // Format as YYYY-MM-DD for input[type="date"]
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }
    
    // Try Excel date serial number
    const excelDate = parseFloat(dateStr);
    if (!isNaN(excelDate) && excelDate > 0) {
      // Excel date serial number (days since 1900-01-01)
      const excelEpoch = new Date(1900, 0, 1);
      const date = new Date(excelEpoch.getTime() + (excelDate - 2) * 24 * 60 * 60 * 1000);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }
    
    return '';
  }

  showModalMessage(title: string, message: string, type: 'success' | 'error' | 'info' | 'warning' = 'info'): void {
    this.modalTitle = title;
    this.modalMessage = message;
    this.modalType = type;
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  parseNumber(value: any): number {
    if (typeof value === 'number') return value;
    if (typeof value === 'string') {
      // Remove currency symbols, commas, spaces
      const cleaned = value.replace(/[₹$,\s]/g, '');
      const num = parseFloat(cleaned);
      return isNaN(num) ? 0 : num;
    }
    return 0;
  }
}

