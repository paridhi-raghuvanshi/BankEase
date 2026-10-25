import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { BankingService } from '../../core/services/banking.service';
import { Account, Beneficiary } from '../../core/models/banking.models';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <a routerLink="/dashboard" class="back-link">← Back to Dashboard</a>
        <h1>Fund Transfer</h1>
        <p>Instant IMPS, NEFT & RTGS Payments with Regulatory Compliance</p>
      </div>

      <div class="content-grid">
        <!-- Transfer Form -->
        <div class="card main-form">
          <h2>Initiate Transfer</h2>

          <div *ngIf="successMessage" class="alert success">{{ successMessage }}</div>
          <div *ngIf="errorMessage" class="alert error">{{ errorMessage }}</div>

          <form (ngSubmit)="onTransfer()" #transferForm="ngForm">
            <div class="form-group">
              <label>Select Debit Account</label>
              <select name="sourceAccount" [(ngModel)]="transferData.sourceAccountNumber" required class="form-control">
                <option value="" disabled selected>Select Account</option>
                <option *ngFor="let acc of accounts" [value]="acc.accountNumber">
                  {{ acc.accountNumber }} ({{ acc.accountType }}) - Available: ₹{{ acc.availableBalance | number:'1.2-2' }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label>Beneficiary / Destination Account</label>
              <div class="beneficiary-picker">
                <select (change)="onSelectBeneficiary($event)" class="form-control mb-2">
                  <option value="">-- Choose from Saved Beneficiaries --</option>
                  <option *ngFor="let b of beneficiaries" [value]="b.accountNumber">
                    {{ b.nickname }} ({{ b.accountHolderName }} - {{ b.bankName }})
                  </option>
                </select>
              </div>
              <input
                type="text"
                name="destAccount"
                [(ngModel)]="transferData.destinationAccountNumber"
                required
                placeholder="Or enter Destination Account Number directly"
                class="form-control"
              />
            </div>

            <div class="form-row">
              <div class="form-group half">
                <label>Amount (₹)</label>
                <input
                  type="number"
                  name="amount"
                  [(ngModel)]="transferData.amount"
                  required
                  min="1"
                  placeholder="0.00"
                  class="form-control"
                />
              </div>

              <div class="form-group half">
                <label>Transfer Mode</label>
                <select name="mode" [(ngModel)]="transferData.mode" required class="form-control">
                  <option value="IMPS">IMPS (Instant, 24x7 - max ₹5 Lakh)</option>
                  <option value="NEFT">NEFT (Standard batch settlement)</option>
                  <option value="RTGS">RTGS (High value - min ₹2 Lakh)</option>
                </select>
              </div>
            </div>

            <div class="form-group">
              <label>Remarks / Purpose</label>
              <input
                type="text"
                name="description"
                [(ngModel)]="transferData.description"
                placeholder="e.g. Monthly rent, Supplier invoice"
                class="form-control"
              />
            </div>

            <button type="submit" [disabled]="!transferForm.form.valid || isSubmitting" class="btn-primary">
              <span *ngIf="!isSubmitting">Verify & Confirm Transfer</span>
              <span *ngIf="isSubmitting">Processing Transaction...</span>
            </button>
          </form>
        </div>

        <!-- Add Beneficiary Panel -->
        <div class="card side-panel">
          <h2>Add New Beneficiary</h2>

          <div *ngIf="beneficiarySuccess" class="alert success">{{ beneficiarySuccess }}</div>

          <form (ngSubmit)="onAddBeneficiary()" #beneficiaryForm="ngForm">
            <div class="form-group">
              <label>Nick Name</label>
              <input type="text" [(ngModel)]="newBeneficiary.nickname" name="nickname" required class="form-control" placeholder="e.g. Landlord" />
            </div>

            <div class="form-group">
              <label>Account Holder Name</label>
              <input type="text" [(ngModel)]="newBeneficiary.accountHolderName" name="accountHolderName" required class="form-control" />
            </div>

            <div class="form-group">
              <label>Account Number</label>
              <input type="text" [(ngModel)]="newBeneficiary.accountNumber" name="bAccountNumber" required class="form-control" />
            </div>

            <div class="form-group">
              <label>IFSC Code</label>
              <input type="text" [(ngModel)]="newBeneficiary.ifscCode" name="ifscCode" required class="form-control" placeholder="e.g. SBIN0001234" />
            </div>

            <div class="form-row">
              <div class="form-group half">
                <label>Bank Name</label>
                <input type="text" [(ngModel)]="newBeneficiary.bankName" name="bankName" required class="form-control" />
              </div>
              <div class="form-group half">
                <label>Branch</label>
                <input type="text" [(ngModel)]="newBeneficiary.branchName" name="branchName" required class="form-control" />
              </div>
            </div>

            <button type="submit" [disabled]="!beneficiaryForm.form.valid || isAddingBeneficiary" class="btn-secondary">
              Save Beneficiary
            </button>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-container {
      padding: 32px 48px;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: #f8fafc;
      min-height: 100vh;
    }
    .back-link {
      color: #2563eb;
      text-decoration: none;
      font-size: 14px;
      font-weight: 600;
      display: inline-block;
      margin-bottom: 8px;
    }
    .page-header h1 {
      margin: 0;
      font-size: 26px;
      color: #0f172a;
    }
    .page-header p {
      margin: 4px 0 24px;
      color: #64748b;
      font-size: 14px;
    }
    .content-grid {
      display: grid;
      grid-template-columns: 3fr 2fr;
      gap: 24px;
    }
    .card {
      background: #ffffff;
      padding: 28px;
      border-radius: 14px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.06);
      border: 1px solid #e2e8f0;
    }
    .card h2 {
      margin: 0 0 20px;
      font-size: 18px;
      color: #1e293b;
    }
    .form-group {
      margin-bottom: 18px;
    }
    .form-group label {
      display: block;
      margin-bottom: 6px;
      font-size: 13px;
      font-weight: 600;
      color: #334155;
    }
    .form-control {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
      font-size: 14px;
      box-sizing: border-box;
    }
    .form-control:focus {
      outline: none;
      border-color: #2563eb;
    }
    .form-row {
      display: flex;
      gap: 12px;
    }
    .form-group.half {
      flex: 1;
    }
    .btn-primary {
      width: 100%;
      padding: 12px;
      background: #2563eb;
      color: #ffffff;
      border: none;
      border-radius: 8px;
      font-size: 15px;
      font-weight: 600;
      cursor: pointer;
    }
    .btn-primary:disabled {
      opacity: 0.6;
    }
    .btn-secondary {
      width: 100%;
      padding: 10px;
      background: #0f172a;
      color: #ffffff;
      border: none;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
    }
    .alert {
      padding: 12px;
      border-radius: 8px;
      font-size: 13px;
      margin-bottom: 16px;
    }
    .alert.success {
      background: #dcfce7;
      color: #166534;
    }
    .alert.error {
      background: #fee2e2;
      color: #991b1b;
    }
    .mb-2 {
      margin-bottom: 8px;
    }
  `]
})
export class TransferComponent implements OnInit {
  accounts: Account[] = [];
  beneficiaries: Beneficiary[] = [];

  transferData = {
    sourceAccountNumber: '',
    destinationAccountNumber: '',
    amount: 0,
    mode: 'IMPS' as 'NEFT' | 'RTGS' | 'IMPS',
    description: ''
  };

  newBeneficiary = {
    nickname: '',
    accountHolderName: '',
    accountNumber: '',
    ifscCode: '',
    bankName: '',
    branchName: ''
  };

  isSubmitting = false;
  isAddingBeneficiary = false;
  successMessage = '';
  errorMessage = '';
  beneficiarySuccess = '';

  constructor(private bankingService: BankingService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.bankingService.getAccounts().subscribe(res => {
      if (res.success) this.accounts = res.data;
    });
    this.bankingService.getBeneficiaries().subscribe(res => {
      if (res.success) this.beneficiaries = res.data;
    });
  }

  onSelectBeneficiary(event: any): void {
    const accNo = event.target.value;
    if (accNo) {
      this.transferData.destinationAccountNumber = accNo;
    }
  }

  onTransfer(): void {
    this.isSubmitting = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.bankingService.transferFunds(this.transferData).subscribe({
      next: (res) => {
        this.isSubmitting = false;
        if (res.success) {
          this.successMessage = `Transfer successful! Reference No: ${res.data.referenceNumber}`;
          this.transferData.amount = 0;
          this.transferData.description = '';
          this.loadData();
        } else {
          this.errorMessage = res.message || 'Transfer failed';
        }
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err.error?.message || 'Transaction error occurred';
      }
    });
  }

  onAddBeneficiary(): void {
    this.isAddingBeneficiary = true;
    this.beneficiarySuccess = '';

    this.bankingService.addBeneficiary(this.newBeneficiary).subscribe({
      next: (res) => {
        this.isAddingBeneficiary = false;
        if (res.success) {
          this.beneficiarySuccess = `Beneficiary "${res.data.nickname}" saved successfully!`;
          this.loadData();
          this.newBeneficiary = { nickname: '', accountHolderName: '', accountNumber: '', ifscCode: '', bankName: '', branchName: '' };
        }
      },
      error: () => {
        this.isAddingBeneficiary = false;
      }
    });
  }
}
