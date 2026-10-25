import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { BankingService } from '../../core/services/banking.service';
import { Account, FixedDeposit, RecurringDeposit } from '../../core/models/banking.models';

@Component({
  selector: 'app-investments',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <a routerLink="/dashboard" class="back-link">← Back to Dashboard</a>
        <h1>Wealth Management & Deposits</h1>
        <p>Fixed Deposits (FD) & Recurring Deposits (RD) with High Yield Guaranteed Returns</p>
      </div>

      <div class="tabs">
        <button [class.active]="activeTab === 'FD'" (click)="activeTab = 'FD'">Fixed Deposit (FD)</button>
        <button [class.active]="activeTab === 'RD'" (click)="activeTab = 'RD'">Recurring Deposit (RD)</button>
      </div>

      <div class="content-grid" *ngIf="activeTab === 'FD'">
        <!-- FD Creation -->
        <div class="card main-form">
          <h2>Open a Fixed Deposit</h2>

          <div *ngIf="fdSuccess" class="alert success">{{ fdSuccess }}</div>
          <div *ngIf="fdError" class="alert error">{{ fdError }}</div>

          <form (ngSubmit)="onCreateFd()" #fdForm="ngForm">
            <div class="form-group">
              <label>Debit Account</label>
              <select [(ngModel)]="fdData.accountNumber" name="account" required class="form-control">
                <option value="" disabled selected>-- Select Account --</option>
                <option *ngFor="let a of accounts" [value]="a.accountNumber">{{ a.accountNumber }} (Bal: ₹{{ a.availableBalance | number }})</option>
              </select>
            </div>

            <div class="form-row">
              <div class="form-group half">
                <label>Deposit Amount (₹)</label>
                <input type="number" [(ngModel)]="fdData.principalAmount" (input)="computeFdEstimate()" name="pAmount" min="1000" required class="form-control" />
              </div>
              <div class="form-group half">
                <label>Tenure (Months)</label>
                <input type="number" [(ngModel)]="fdData.tenureMonths" (input)="computeFdEstimate()" name="tMonths" min="3" max="120" required class="form-control" />
              </div>
            </div>

            <div class="form-group">
              <label>Interest Payout</label>
              <select [(ngModel)]="fdData.interestPayoutType" name="payout" class="form-control">
                <option value="ON_MATURITY">Cumulative (On Maturity - Compounded Quarterly)</option>
                <option value="MONTHLY">Monthly Payout</option>
                <option value="QUARTERLY">Quarterly Payout</option>
              </select>
            </div>

            <button type="submit" [disabled]="!fdForm.form.valid || isSubmitting" class="btn-primary">
              {{ isSubmitting ? 'Opening FD...' : 'Open Fixed Deposit Instantly' }}
            </button>
          </form>

          <!-- Active FDs -->
          <div class="portfolio-list" *ngIf="fixedDeposits.length > 0">
            <h3>Your Fixed Deposits</h3>
            <div class="item-grid">
              <div *ngFor="let fd of fixedDeposits" class="portfolio-card">
                <div class="card-top">
                  <strong>{{ fd.fdNumber }}</strong>
                  <span class="badge">{{ fd.status }}</span>
                </div>
                <p>Principal: <strong>₹{{ fd.principalAmount | number }}</strong></p>
                <p>Maturity Value: <strong class="highlight">₹{{ fd.maturityAmount | number:'1.2-2' }}</strong></p>
                <p>Rate: {{ fd.interestRate }}% | Maturity: {{ fd.maturityDate | date }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- FD Yield Forecast -->
        <div class="card side-panel">
          <h2>Guaranteed Returns</h2>
          <div class="estimate-box">
            <span class="est-label">Estimated Maturity Payout</span>
            <div class="est-amount">₹{{ estimatedFdMaturity | number:'1.2-2' }}</div>
            <p>Annual Return: <strong>7.10% p.a.</strong></p>
            <small>Quarterly compound interest applied automatically as per Reserve Bank guidelines.</small>
          </div>
        </div>
      </div>

      <div class="content-grid" *ngIf="activeTab === 'RD'">
        <!-- RD Creation -->
        <div class="card main-form">
          <h2>Open a Recurring Deposit</h2>

          <div *ngIf="rdSuccess" class="alert success">{{ rdSuccess }}</div>
          <div *ngIf="rdError" class="alert error">{{ rdError }}</div>

          <form (ngSubmit)="onCreateRd()" #rdForm="ngForm">
            <div class="form-group">
              <label>Debit Account</label>
              <select [(ngModel)]="rdData.accountNumber" name="account" required class="form-control">
                <option value="" disabled selected>-- Select Account --</option>
                <option *ngFor="let a of accounts" [value]="a.accountNumber">{{ a.accountNumber }} (Bal: ₹{{ a.availableBalance | number }})</option>
              </select>
            </div>

            <div class="form-row">
              <div class="form-group half">
                <label>Monthly Installment (₹)</label>
                <input type="number" [(ngModel)]="rdData.monthlyInstallment" name="installment" min="500" required class="form-control" />
              </div>
              <div class="form-group half">
                <label>Tenure (Months)</label>
                <input type="number" [(ngModel)]="rdData.tenureMonths" name="tMonths" min="6" max="120" required class="form-control" />
              </div>
            </div>

            <button type="submit" [disabled]="!rdForm.form.valid || isSubmitting" class="btn-primary">
              {{ isSubmitting ? 'Opening RD...' : 'Start Systematic RD' }}
            </button>
          </form>

          <!-- Active RDs -->
          <div class="portfolio-list" *ngIf="recurringDeposits.length > 0">
            <h3>Your Recurring Deposits</h3>
            <div class="item-grid">
              <div *ngFor="let rd of recurringDeposits" class="portfolio-card">
                <div class="card-top">
                  <strong>{{ rd.rdNumber }}</strong>
                  <span class="badge">{{ rd.status }}</span>
                </div>
                <p>Monthly Debit: <strong>₹{{ rd.monthlyInstallment | number }}</strong></p>
                <p>Installments Paid: {{ rd.installmentsPaid }} / {{ rd.tenureMonths }}</p>
                <p>Target Maturity: <strong class="highlight">₹{{ rd.maturityAmount | number:'1.2-2' }}</strong></p>
              </div>
            </div>
          </div>
        </div>

        <div class="card side-panel">
          <h2>Systematic Savings</h2>
          <div class="estimate-box">
            <span class="est-label">Standard RD Rate</span>
            <div class="est-amount">6.90% p.a.</div>
            <p>Discipline your monthly financial planning with automated debits on the 1st of every month.</p>
          </div>
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
    .tabs {
      display: flex;
      gap: 8px;
      margin-bottom: 24px;
    }
    .tabs button {
      padding: 10px 20px;
      border: 1px solid #cbd5e1;
      background: #ffffff;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 600;
      color: #475569;
      cursor: pointer;
    }
    .tabs button.active {
      background: #0f172a;
      color: #ffffff;
      border-color: #0f172a;
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
    .estimate-box {
      background: #f1f5f9;
      padding: 24px;
      border-radius: 12px;
      text-align: center;
    }
    .est-amount {
      font-size: 32px;
      font-weight: 800;
      color: #16a34a;
      margin: 8px 0;
    }
    .portfolio-list {
      margin-top: 32px;
      padding-top: 24px;
      border-top: 1px solid #e2e8f0;
    }
    .item-grid {
      display: grid;
      gap: 12px;
    }
    .portfolio-card {
      background: #f8fafc;
      padding: 16px;
      border-radius: 8px;
      border: 1px solid #e2e8f0;
    }
    .card-top {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
    }
    .highlight {
      color: #16a34a;
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
export class InvestmentsComponent implements OnInit {
  activeTab: 'FD' | 'RD' = 'FD';
  accounts: Account[] = [];
  fixedDeposits: FixedDeposit[] = [];
  recurringDeposits: RecurringDeposit[] = [];

  fdData = {
    accountNumber: '',
    principalAmount: 25000,
    tenureMonths: 12,
    interestPayoutType: 'ON_MATURITY'
  };

  rdData = {
    accountNumber: '',
    monthlyInstallment: 2000,
    tenureMonths: 12
  };

  estimatedFdMaturity = 0;
  isSubmitting = false;
  fdSuccess = '';
  fdError = '';
  rdSuccess = '';
  rdError = '';

  constructor(private bankingService: BankingService) {}

  ngOnInit(): void {
    this.loadData();
    this.computeFdEstimate();
  }

  loadData(): void {
    this.bankingService.getAccounts().subscribe(res => {
      if (res.success) this.accounts = res.data;
    });
    this.bankingService.getFixedDeposits().subscribe(res => {
      if (res.success) this.fixedDeposits = res.data;
    });
    this.bankingService.getRecurringDeposits().subscribe(res => {
      if (res.success) this.recurringDeposits = res.data;
    });
  }

  computeFdEstimate(): void {
    const P = this.fdData.principalAmount || 0;
    const n = (this.fdData.tenureMonths || 0) / 3.0;
    const r = 0.071 / 4.0;
    if (P > 0) {
      this.estimatedFdMaturity = Math.round(P * Math.pow(1 + r, n));
    }
  }

  onCreateFd(): void {
    this.isSubmitting = true;
    this.fdSuccess = '';
    this.fdError = '';

    this.bankingService.createFixedDeposit(this.fdData).subscribe({
      next: (res) => {
        this.isSubmitting = false;
        if (res.success) {
          this.fdSuccess = `Fixed Deposit created! Account Ref: ${res.data.fdNumber}`;
          this.loadData();
        }
      },
      error: (err) => {
        this.isSubmitting = false;
        this.fdError = err.error?.message || 'Failed to create Fixed Deposit.';
      }
    });
  }

  onCreateRd(): void {
    this.isSubmitting = true;
    this.rdSuccess = '';
    this.rdError = '';

    this.bankingService.createRecurringDeposit(this.rdData).subscribe({
      next: (res) => {
        this.isSubmitting = false;
        if (res.success) {
          this.rdSuccess = `Recurring Deposit created! Account Ref: ${res.data.rdNumber}`;
          this.loadData();
        }
      },
      error: (err) => {
        this.isSubmitting = false;
        this.rdError = err.error?.message || 'Failed to create Recurring Deposit.';
      }
    });
  }
}
