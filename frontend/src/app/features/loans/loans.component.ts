import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { BankingService } from '../../core/services/banking.service';
import { Account, Loan } from '../../core/models/banking.models';

@Component({
  selector: 'app-loans',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <a routerLink="/dashboard" class="back-link">← Back to Dashboard</a>
        <h1>Retail & Commercial Loans</h1>
        <p>Instant Pre-approvals, EMI Calculators, and Application Tracker</p>
      </div>

      <div class="content-grid">
        <!-- Loan Application Form -->
        <div class="card main-form">
          <h2>Apply for a New Loan</h2>

          <div *ngIf="applySuccess" class="alert success">{{ applySuccess }}</div>
          <div *ngIf="applyError" class="alert error">{{ applyError }}</div>

          <form (ngSubmit)="onApplyLoan()" #loanForm="ngForm">
            <div class="form-row">
              <div class="form-group half">
                <label>Loan Product</label>
                <select [(ngModel)]="loanData.loanType" name="loanType" (change)="updateInterest()" required class="form-control">
                  <option value="HOME">Home Loan (8.50% p.a.)</option>
                  <option value="PERSONAL">Personal Loan (11.50% p.a.)</option>
                  <option value="CAR">Vehicle / Auto Loan (9.00% p.a.)</option>
                  <option value="EDUCATION">Education Loan (8.00% p.a.)</option>
                  <option value="GOLD">Gold Loan (7.50% p.a.)</option>
                  <option value="BUSINESS">Business Expansion Loan (12.00% p.a.)</option>
                </select>
              </div>

              <div class="form-group half">
                <label>Disbursement Account</label>
                <select [(ngModel)]="loanData.accountNumber" name="account" required class="form-control">
                  <option value="" disabled selected>Select Account</option>
                  <option *ngFor="let a of accounts" [value]="a.accountNumber">{{ a.accountNumber }}</option>
                </select>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group half">
                <label>Principal Amount (₹)</label>
                <input type="number" [(ngModel)]="loanData.principalAmount" (input)="recalculateEmi()" name="pAmount" min="10000" required class="form-control" />
              </div>
              <div class="form-group half">
                <label>Tenure (Months)</label>
                <input type="number" [(ngModel)]="loanData.tenureMonths" (input)="recalculateEmi()" name="tMonths" min="6" max="360" required class="form-control" />
              </div>
            </div>

            <div class="form-group">
              <label>Purpose of Loan</label>
              <input type="text" [(ngModel)]="loanData.purpose" name="purpose" placeholder="e.g. Home Renovation, New Car Purchase" class="form-control" />
            </div>

            <button type="submit" [disabled]="!loanForm.form.valid || isApplying" class="btn-primary">
              {{ isApplying ? 'Submitting Application...' : 'Submit Loan Application' }}
            </button>
          </form>

          <!-- Existing Active Loans -->
          <div class="existing-loans" *ngIf="loans.length > 0">
            <h3>Your Loan Portfolio</h3>
            <div class="loan-cards">
              <div *ngFor="let l of loans" class="loan-card">
                <div class="loan-header">
                  <strong>{{ l.loanType }} LOAN</strong>
                  <span class="badge">{{ l.status }}</span>
                </div>
                <div class="loan-body">
                  <p>Loan No: <code>{{ l.loanNumber }}</code></p>
                  <p>Principal: <strong>₹{{ l.principalAmount | number }}</strong></p>
                  <p>Monthly EMI: <strong class="emi-text">₹{{ l.emiAmount | number:'1.2-2' }}</strong></p>
                  <p>Tenure: {{ l.tenureMonths }} months (Next Due: {{ l.nextEmiDate | date }})</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Interactive EMI Calculator Panel -->
        <div class="card side-panel">
          <h2>Interactive EMI Calculator</h2>
          <div class="calculator-box">
            <div class="calc-row">
              <label>Estimated Monthly EMI:</label>
              <div class="calc-emi">₹{{ calculatedEmi | number:'1.2-2' }}</div>
            </div>
            <div class="calc-stats">
              <div class="stat">
                <small>Total Interest</small>
                <strong>₹{{ calculatedInterest | number:'1.2-2' }}</strong>
              </div>
              <div class="stat">
                <small>Total Payment</small>
                <strong>₹{{ calculatedTotal | number:'1.2-2' }}</strong>
              </div>
            </div>
            <div class="formula-note">
              Formula: <code>E = P × r × (1+r)ⁿ / ((1+r)ⁿ - 1)</code>
            </div>
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
    .calculator-box {
      background: #f1f5f9;
      padding: 24px;
      border-radius: 12px;
    }
    .calc-row {
      text-align: center;
      margin-bottom: 20px;
    }
    .calc-emi {
      font-size: 32px;
      font-weight: 800;
      color: #2563eb;
      margin-top: 6px;
    }
    .calc-stats {
      display: flex;
      justify-content: space-around;
      padding: 16px 0;
      border-top: 1px solid #e2e8f0;
      border-bottom: 1px solid #e2e8f0;
    }
    .stat small {
      display: block;
      color: #64748b;
    }
    .stat strong {
      font-size: 16px;
      color: #0f172a;
    }
    .formula-note {
      margin-top: 16px;
      font-size: 11px;
      color: #64748b;
      text-align: center;
    }
    .existing-loans {
      margin-top: 32px;
      padding-top: 24px;
      border-top: 1px solid #e2e8f0;
    }
    .existing-loans h3 {
      font-size: 16px;
      margin-bottom: 16px;
    }
    .loan-cards {
      display: grid;
      gap: 12px;
    }
    .loan-card {
      background: #f8fafc;
      padding: 16px;
      border-radius: 8px;
      border: 1px solid #e2e8f0;
    }
    .loan-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
    }
    .emi-text {
      color: #2563eb;
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
export class LoansComponent implements OnInit {
  accounts: Account[] = [];
  loans: Loan[] = [];

  loanData = {
    loanType: 'HOME',
    accountNumber: '',
    principalAmount: 500000,
    tenureMonths: 60,
    purpose: ''
  };

  currentRate = 8.5;
  calculatedEmi = 0;
  calculatedInterest = 0;
  calculatedTotal = 0;

  isApplying = false;
  applySuccess = '';
  applyError = '';

  constructor(private bankingService: BankingService) {}

  ngOnInit(): void {
    this.loadData();
    this.recalculateEmi();
  }

  loadData(): void {
    this.bankingService.getAccounts().subscribe(res => {
      if (res.success) this.accounts = res.data;
    });
    this.bankingService.getLoans().subscribe(res => {
      if (res.success) this.loans = res.data;
    });
  }

  updateInterest(): void {
    switch (this.loanData.loanType) {
      case 'HOME': this.currentRate = 8.5; break;
      case 'PERSONAL': this.currentRate = 11.5; break;
      case 'CAR': this.currentRate = 9.0; break;
      case 'EDUCATION': this.currentRate = 8.0; break;
      case 'GOLD': this.currentRate = 7.5; break;
      case 'BUSINESS': this.currentRate = 12.0; break;
    }
    this.recalculateEmi();
  }

  recalculateEmi(): void {
    const P = this.loanData.principalAmount || 0;
    const n = this.loanData.tenureMonths || 1;
    const r = (this.currentRate / 100) / 12;

    if (P > 0 && n > 0) {
      const emi = (P * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
      this.calculatedEmi = Math.round(emi);
      this.calculatedTotal = Math.round(emi * n);
      this.calculatedInterest = Math.round(this.calculatedTotal - P);
    }
  }

  onApplyLoan(): void {
    this.isApplying = true;
    this.applySuccess = '';
    this.applyError = '';

    this.bankingService.applyForLoan(this.loanData).subscribe({
      next: (res) => {
        this.isApplying = false;
        if (res.success) {
          this.applySuccess = `Loan Application submitted! Loan Ref: ${res.data.loanNumber}`;
          this.loadData();
        }
      },
      error: (err) => {
        this.isApplying = false;
        this.applyError = err.error?.message || 'Loan application failed';
      }
    });
  }
}
