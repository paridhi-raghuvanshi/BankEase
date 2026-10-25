import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="auth-container">
      <div class="auth-card">
        <div class="brand">
          <div class="logo">🏦</div>
          <h2>BankEase</h2>
          <p>NextGen Net Banking Portal</p>
        </div>

        <form (ngSubmit)="onLogin()" #loginForm="ngForm">
          <div *ngIf="errorMessage" class="alert error">
            {{ errorMessage }}
          </div>

          <div class="form-group">
            <label for="email">Registered Email</label>
            <input
              type="email"
              id="email"
              name="email"
              [(ngModel)]="email"
              required
              placeholder="e.g. rajesh@example.com"
              class="form-control"
            />
          </div>

          <div class="form-group">
            <label for="password">Password</label>
            <input
              type="password"
              id="password"
              name="password"
              [(ngModel)]="password"
              required
              placeholder="••••••••"
              class="form-control"
            />
          </div>

          <button type="submit" [disabled]="!loginForm.form.valid || isLoading" class="btn-primary">
            <span *ngIf="!isLoading">Secure Sign In</span>
            <span *ngIf="isLoading">Authenticating...</span>
          </button>
        </form>

        <div class="demo-hints">
          <p><strong>Demo Accounts:</strong></p>
          <p>Customer: <code>rajesh&#64;example.com</code> / <code>Customer&#64;123</code></p>
          <p>Admin: <code>admin&#64;bankease.com</code> / <code>Admin&#64;12345</code></p>
        </div>

        <div class="auth-footer">
          <p>New to BankEase? <a routerLink="/register">Register here</a></p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      padding: 20px;
    }
    .auth-card {
      background: #ffffff;
      border-radius: 16px;
      padding: 40px;
      width: 100%;
      max-width: 440px;
      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2), 0 10px 10px -5px rgba(0, 0, 0, 0.1);
    }
    .brand {
      text-align: center;
      margin-bottom: 28px;
    }
    .brand .logo {
      font-size: 40px;
      margin-bottom: 8px;
    }
    .brand h2 {
      margin: 0;
      color: #0f172a;
      font-size: 26px;
      font-weight: 700;
    }
    .brand p {
      margin: 6px 0 0;
      color: #64748b;
      font-size: 14px;
    }
    .form-group {
      margin-bottom: 20px;
    }
    .form-group label {
      display: block;
      margin-bottom: 8px;
      font-size: 13px;
      font-weight: 600;
      color: #334155;
    }
    .form-control {
      width: 100%;
      padding: 12px 14px;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
      font-size: 14px;
      box-sizing: border-box;
      transition: border-color 0.2s;
    }
    .form-control:focus {
      outline: none;
      border-color: #2563eb;
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
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
      transition: background 0.2s;
    }
    .btn-primary:hover:not(:disabled) {
      background: #1d4ed8;
    }
    .btn-primary:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .alert.error {
      background: #fee2e2;
      color: #991b1b;
      padding: 12px;
      border-radius: 8px;
      font-size: 14px;
      margin-bottom: 20px;
    }
    .demo-hints {
      margin-top: 24px;
      background: #f8fafc;
      border: 1px dashed #cbd5e1;
      padding: 12px;
      border-radius: 8px;
      font-size: 12px;
      color: #475569;
    }
    .demo-hints p {
      margin: 4px 0;
    }
    .demo-hints code {
      background: #e2e8f0;
      padding: 2px 4px;
      border-radius: 4px;
    }
    .auth-footer {
      text-align: center;
      margin-top: 24px;
      font-size: 14px;
      color: #64748b;
    }
    .auth-footer a {
      color: #2563eb;
      text-decoration: none;
      font-weight: 600;
    }
  `]
})
export class LoginComponent {
  email = '';
  password = '';
  isLoading = false;
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  onLogin(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.success) {
          this.router.navigate(['/dashboard']);
        } else {
          this.errorMessage = res.message || 'Login failed';
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Invalid credentials or account locked';
      }
    });
  }
}
