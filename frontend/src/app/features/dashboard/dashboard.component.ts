import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { BankingService } from '../../core/services/banking.service';
import { Account, Transaction, UserSummary } from '../../core/models/banking.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="dashboard-layout">
      <!-- Sidebar -->
      <aside class="sidebar">
        <div class="brand">
          <span class="logo">🏦</span>
          <h3>BankEase</h3>
        </div>
        <nav class="nav-links">
          <a routerLink="/dashboard" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">📊 Overview</a>
          <a routerLink="/transfer" routerLinkActive="active">💸 Fund Transfer</a>
          <a routerLink="/bills" routerLinkActive="active">🧾 Bill Pay</a>
          <a routerLink="/loans" routerLinkActive="active">💳 Loans & EMI</a>
          <a routerLink="/investments" routerLinkActive="active">📈 Fixed / Recurring Deposits</a>
        </nav>
        <div class="user-profile">
          <div class="avatar">{{ user?.firstName?.charAt(0) || 'U' }}</div>
          <div class="user-meta">
            <strong>{{ user?.firstName }} {{ user?.lastName }}</strong>
            <small>{{ user?.email }}</small>
          </div>
          <button (click)="logout()" class="btn-logout" title="Logout">🚪</button>
        </div>
      </aside>

      <!-- Main Content Area -->
      <main class="main-content">
        <header class="topbar">
          <div>
            <h1>Welcome back, {{ user?.firstName }}! 👋</h1>
            <p>Customer ID: <code>{{ user?.customerId }}</code> | Security Status: <span class="badge-success">Encrypted</span></p>
          </div>
          <div class="quick-cta">
            <a routerLink="/transfer" class="btn-action">Make a Transfer</a>
          </div>
        </header>

        <!-- Accounts Grid -->
        <section class="section">
          <h2>Your Bank Accounts</h2>
          <div *ngIf="loadingAccounts" class="loading">Loading accounts...</div>

          <div *ngIf="!loadingAccounts && accounts.length === 0" class="empty-card">
            No active accounts found. Contact bank branch.
          </div>

          <div class="account-cards">
            <div *ngFor="let acc of accounts" class="account-card" [class.primary]="acc.accountType === 'SAVINGS'">
              <div class="card-header">
                <span class="acc-type">{{ acc.accountType }} ACCOUNT</span>
                <span class="badge">{{ acc.status }}</span>
              </div>
              <div class="balance-block">
                <span class="curr">₹</span>
                <span class="amount">{{ acc.balance | number:'1.2-2' }}</span>
              </div>
              <div class="card-meta">
                <div>
                  <small>Account Number</small>
                  <strong>{{ acc.accountNumber }}</strong>
                </div>
                <div>
                  <small>IFSC Code</small>
                  <strong>{{ acc.ifscCode }}</strong>
                </div>
              </div>
              <div class="card-footer">
                <span>Daily Limit: ₹{{ acc.dailyTransferLimit | number }}</span>
                <button (click)="selectAccount(acc)" class="btn-sm">View History</button>
              </div>
            </div>
          </div>
        </section>

        <!-- Recent Transactions -->
        <section class="section" *ngIf="selectedAccount">
          <div class="section-header">
            <h2>Recent Transactions: {{ selectedAccount.accountNumber }}</h2>
            <button (click)="loadStatement(selectedAccount.accountNumber)" class="btn-refresh">🔄 Refresh</button>
          </div>

          <div *ngIf="loadingTransactions" class="loading">Loading transaction log...</div>

          <div *ngIf="!loadingTransactions && transactions.length === 0" class="empty-card">
            No recent transactions recorded for this account.
          </div>

          <table *ngIf="!loadingTransactions && transactions.length > 0" class="data-table">
            <thead>
              <tr>
                <th>Date & Time</th>
                <th>Reference</th>
                <th>Description</th>
                <th>Mode</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let txn of transactions">
                <td>{{ txn.createdAt | date:'short' }}</td>
                <td><code>{{ txn.referenceNumber }}</code></td>
                <td>{{ txn.description || 'Fund Transfer' }}</td>
                <td><span class="mode-pill">{{ txn.mode }}</span></td>
                <td>
                  <span class="type-pill" [class.credit]="txn.type === 'CREDIT'" [class.debit]="txn.type === 'DEBIT'">
                    {{ txn.type }}
                  </span>
                </td>
                <td class="amount-cell" [class.positive]="txn.type === 'CREDIT'" [class.negative]="txn.type === 'DEBIT'">
                  {{ txn.type === 'CREDIT' ? '+' : '-' }}₹{{ txn.amount | number:'1.2-2' }}
                </td>
                <td><span class="status-success">{{ txn.status }}</span></td>
              </tr>
            </tbody>
          </table>
        </section>
      </main>
    </div>
  `,
  styles: [`
    .dashboard-layout {
      display: flex;
      min-height: 100vh;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: #f1f5f9;
    }
    .sidebar {
      width: 260px;
      background: #0f172a;
      color: #ffffff;
      display: flex;
      flex-direction: column;
      padding: 24px;
      flex-shrink: 0;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 32px;
    }
    .brand .logo {
      font-size: 28px;
    }
    .brand h3 {
      margin: 0;
      font-size: 20px;
      font-weight: 700;
    }
    .nav-links {
      display: flex;
      flex-direction: column;
      gap: 8px;
      flex: 1;
    }
    .nav-links a {
      color: #94a3b8;
      text-decoration: none;
      padding: 12px 16px;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 500;
      transition: all 0.2s;
    }
    .nav-links a:hover, .nav-links a.active {
      background: #1e293b;
      color: #38bdf8;
    }
    .user-profile {
      display: flex;
      align-items: center;
      gap: 12px;
      padding-top: 16px;
      border-top: 1px solid #334155;
    }
    .avatar {
      width: 36px;
      height: 36px;
      background: #38bdf8;
      color: #0f172a;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
    }
    .user-meta {
      flex: 1;
      overflow: hidden;
    }
    .user-meta strong {
      display: block;
      font-size: 13px;
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;
    }
    .user-meta small {
      color: #94a3b8;
      font-size: 11px;
    }
    .btn-logout {
      background: transparent;
      border: none;
      font-size: 18px;
      cursor: pointer;
    }
    .main-content {
      flex: 1;
      padding: 32px 40px;
      overflow-y: auto;
    }
    .topbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 32px;
    }
    .topbar h1 {
      margin: 0;
      font-size: 24px;
      color: #0f172a;
    }
    .topbar p {
      margin: 6px 0 0;
      color: #64748b;
      font-size: 13px;
    }
    .badge-success {
      background: #dcfce7;
      color: #166534;
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: 600;
    }
    .btn-action {
      background: #2563eb;
      color: white;
      text-decoration: none;
      padding: 10px 18px;
      border-radius: 8px;
      font-weight: 600;
      font-size: 14px;
    }
    .section {
      margin-bottom: 36px;
    }
    .section h2 {
      margin: 0 0 16px;
      font-size: 18px;
      color: #1e293b;
    }
    .account-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 20px;
    }
    .account-card {
      background: #ffffff;
      border-radius: 14px;
      padding: 24px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
      border: 1px solid #e2e8f0;
    }
    .account-card.primary {
      border-top: 4px solid #2563eb;
    }
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    .acc-type {
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.5px;
      color: #64748b;
    }
    .balance-block {
      margin-bottom: 20px;
    }
    .balance-block .curr {
      font-size: 20px;
      font-weight: 600;
      color: #64748b;
      margin-right: 4px;
    }
    .balance-block .amount {
      font-size: 32px;
      font-weight: 700;
      color: #0f172a;
    }
    .card-meta {
      display: flex;
      justify-content: space-between;
      padding-top: 16px;
      border-top: 1px solid #f1f5f9;
      margin-bottom: 16px;
    }
    .card-meta small {
      display: block;
      color: #64748b;
      font-size: 11px;
    }
    .card-meta strong {
      font-size: 13px;
      color: #334155;
    }
    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
      color: #64748b;
    }
    .btn-sm {
      background: #f1f5f9;
      border: 1px solid #cbd5e1;
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
    }
    .data-table {
      width: 100%;
      background: #ffffff;
      border-radius: 12px;
      border-collapse: collapse;
      overflow: hidden;
      box-shadow: 0 1px 3px rgba(0,0,0,0.05);
    }
    .data-table th, .data-table td {
      padding: 14px 16px;
      text-align: left;
      font-size: 13px;
      border-bottom: 1px solid #f1f5f9;
    }
    .data-table th {
      background: #f8fafc;
      color: #64748b;
      font-weight: 600;
    }
    .amount-cell {
      font-weight: 700;
    }
    .amount-cell.positive {
      color: #16a34a;
    }
    .amount-cell.negative {
      color: #dc2626;
    }
    .mode-pill {
      background: #e2e8f0;
      padding: 3px 6px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
    }
    .type-pill.credit {
      color: #16a34a;
      font-weight: 700;
    }
    .type-pill.debit {
      color: #dc2626;
      font-weight: 700;
    }
    .status-success {
      color: #16a34a;
      font-weight: 600;
    }
    .empty-card {
      background: #ffffff;
      padding: 24px;
      border-radius: 8px;
      color: #64748b;
      font-size: 14px;
    }
    .btn-refresh {
      background: transparent;
      border: 1px solid #cbd5e1;
      padding: 6px 12px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 12px;
    }
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
  `]
})
export class DashboardComponent implements OnInit {
  user: UserSummary | null = null;
  accounts: Account[] = [];
  selectedAccount: Account | null = null;
  transactions: Transaction[] = [];
  loadingAccounts = false;
  loadingTransactions = false;

  constructor(private authService: AuthService, private bankingService: BankingService) {}

  ngOnInit(): void {
    this.user = this.authService.currentUserValue;
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.loadingAccounts = true;
    this.bankingService.getAccounts().subscribe({
      next: (res) => {
        this.loadingAccounts = false;
        if (res.success && res.data) {
          this.accounts = res.data;
          if (this.accounts.length > 0) {
            this.selectAccount(this.accounts[0]);
          }
        }
      },
      error: () => {
        this.loadingAccounts = false;
      }
    });
  }

  selectAccount(acc: Account): void {
    this.selectedAccount = acc;
    this.loadStatement(acc.accountNumber);
  }

  loadStatement(accountNumber: string): void {
    this.loadingTransactions = true;
    this.bankingService.getAccountStatement(accountNumber).subscribe({
      next: (res) => {
        this.loadingTransactions = false;
        if (res.success && res.data) {
          this.transactions = res.data.content;
        }
      },
      error: () => {
        this.loadingTransactions = false;
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }
}
