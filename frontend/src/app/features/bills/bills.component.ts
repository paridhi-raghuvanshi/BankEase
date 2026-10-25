import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { BankingService } from '../../core/services/banking.service';
import { Account, Biller, BillPayment } from '../../core/models/banking.models';

@Component({
  selector: 'app-bills',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <a routerLink="/dashboard" class="back-link">← Back to Dashboard</a>
        <h1>Bill Payments & Utilities</h1>
        <p>Electricity, Water, Mobile Postpaid, Broadband & DTH Services</p>
      </div>

      <div class="content-grid">
        <!-- Pay Bill Form -->
        <div class="card main-form">
          <h2>Pay Utility Bill</h2>

          <div *ngIf="paySuccess" class="alert success">{{ paySuccess }}</div>
          <div *ngIf="payError" class="alert error">{{ payError }}</div>

          <form (ngSubmit)="onPayBill()" #billForm="ngForm">
            <div class="form-group">
              <label>Select Registered Biller</label>
              <select name="biller" [(ngModel)]="paymentData.billerId" required class="form-control">
                <option [ngValue]="0" disabled selected>-- Choose Biller --</option>
                <option *ngFor="let b of billers" [ngValue]="b.id">
                  {{ b.billerName }} ({{ b.category }}) - Consumer ID: {{ b.consumerId }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label>Debit Account</label>
              <select name="acc" [(ngModel)]="paymentData.accountNumber" required class="form-control">
                <option value="" disabled selected>-- Select Account --</option>
                <option *ngFor="let a of accounts" [value]="a.accountNumber">
                  {{ a.accountNumber }} - Balance: ₹{{ a.availableBalance | number:'1.2-2' }}
                </option>
              </select>
            </div>

            <div class="form-row">
              <div class="form-group half">
                <label>Billing Period</label>
                <input type="text" [(ngModel)]="paymentData.billPeriod" name="period" required placeholder="e.g. Oct 2026" class="form-control" />
              </div>
              <div class="form-group half">
                <label>Amount (₹)</label>
                <input type="number" [(ngModel)]="paymentData.amount" name="amount" required min="1" placeholder="0.00" class="form-control" />
              </div>
            </div>

            <button type="submit" [disabled]="!billForm.form.valid || isPaying" class="btn-primary">
              {{ isPaying ? 'Processing Payment...' : 'Pay Bill Instantly' }}
            </button>
          </form>
        </div>

        <!-- Add Biller Form -->
        <div class="card side-panel">
          <h2>Register New Biller</h2>

          <div *ngIf="addBillerSuccess" class="alert success">{{ addBillerSuccess }}</div>

          <form (ngSubmit)="onAddBiller()" #addBillerForm="ngForm">
            <div class="form-group">
              <label>Category</label>
              <select [(ngModel)]="newBiller.category" name="category" required class="form-control">
                <option value="ELECTRICITY">Electricity</option>
                <option value="WATER">Water</option>
                <option value="GAS">Piped Gas / LPG</option>
                <option value="BROADBAND">Broadband / Internet</option>
                <option value="MOBILE_POSTPAID">Mobile Postpaid</option>
                <option value="DTH">DTH Cable</option>
                <option value="INSURANCE">Insurance Premium</option>
              </select>
            </div>

            <div class="form-group">
              <label>Biller Organization Name</label>
              <input type="text" [(ngModel)]="newBiller.billerName" name="billerName" required placeholder="e.g. BESCOM / Adani Gas" class="form-control" />
            </div>

            <div class="form-group">
              <label>Consumer Number / Account ID</label>
              <input type="text" [(ngModel)]="newBiller.consumerId" name="consumerId" required class="form-control" />
            </div>

            <div class="form-group">
              <label>Consumer Name on Bill</label>
              <input type="text" [(ngModel)]="newBiller.consumerName" name="consumerName" class="form-control" />
            </div>

            <button type="submit" [disabled]="!addBillerForm.form.valid || isAddingBiller" class="btn-secondary">
              Save Biller
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
  `]
})
export class BillsComponent implements OnInit {
  accounts: Account[] = [];
  billers: Biller[] = [];

  paymentData = {
    billerId: 0,
    accountNumber: '',
    amount: 0,
    billPeriod: ''
  };

  newBiller = {
    category: 'ELECTRICITY',
    billerName: '',
    consumerId: '',
    consumerName: ''
  };

  isPaying = false;
  isAddingBiller = false;
  paySuccess = '';
  payError = '';
  addBillerSuccess = '';

  constructor(private bankingService: BankingService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.bankingService.getAccounts().subscribe(res => {
      if (res.success) this.accounts = res.data;
    });
    this.bankingService.getBillers().subscribe(res => {
      if (res.success) this.billers = res.data;
    });
  }

  onPayBill(): void {
    this.isPaying = true;
    this.paySuccess = '';
    this.payError = '';

    this.bankingService.payBill(this.paymentData).subscribe({
      next: (res) => {
        this.isPaying = false;
        if (res.success) {
          this.paySuccess = `Bill paid successfully! Confirmation Ref: ${res.data.confirmationNumber}`;
          this.paymentData.amount = 0;
          this.paymentData.billPeriod = '';
          this.loadData();
        }
      },
      error: (err) => {
        this.isPaying = false;
        this.payError = err.error?.message || 'Bill payment failed.';
      }
    });
  }

  onAddBiller(): void {
    this.isAddingBiller = true;
    this.addBillerSuccess = '';

    this.bankingService.addBiller(this.newBiller).subscribe({
      next: (res) => {
        this.isAddingBiller = false;
        if (res.success) {
          this.addBillerSuccess = `Biller "${res.data.billerName}" registered!`;
          this.loadData();
          this.newBiller = { category: 'ELECTRICITY', billerName: '', consumerId: '', consumerName: '' };
        }
      },
      error: () => {
        this.isAddingBiller = false;
      }
    });
  }
}
