import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="auth-container">
      <div class="auth-card">
        <div class="brand">
          <div class="logo">🏦</div>
          <h2>Join BankEase</h2>
          <p>Instant NextGen Net Banking Registration</p>
        </div>

        <form (ngSubmit)="onRegister()" #regForm="ngForm">
          <div *ngIf="errorMessage" class="alert error">
            {{ errorMessage }}
          </div>

          <div class="form-row">
            <div class="form-group half">
              <label for="firstName">First Name</label>
              <input type="text" id="firstName" name="firstName" [(ngModel)]="model.firstName" required class="form-control" />
            </div>
            <div class="form-group half">
              <label for="lastName">Last Name</label>
              <input type="text" id="lastName" name="lastName" [(ngModel)]="model.lastName" required class="form-control" />
            </div>
          </div>

          <div class="form-group">
            <label for="email">Email Address</label>
            <input type="email" id="email" name="email" [(ngModel)]="model.email" required class="form-control" />
          </div>

          <div class="form-row">
            <div class="form-group half">
              <label for="phone">Phone Number</label>
              <input type="tel" id="phone" name="phone" [(ngModel)]="model.phone" required pattern="[0-9]{10}" placeholder="10 digits" class="form-control" />
            </div>
            <div class="form-group half">
              <label for="password">Password</label>
              <input type="password" id="password" name="password" [(ngModel)]="model.password" required minlength="8" class="form-control" />
            </div>
          </div>

          <div class="form-group">
            <label for="address">Address</label>
            <input type="text" id="address" name="address" [(ngModel)]="model.address" required class="form-control" />
          </div>

          <div class="form-row">
            <div class="form-group third">
              <label for="city">City</label>
              <input type="text" id="city" name="city" [(ngModel)]="model.city" required class="form-control" />
            </div>
            <div class="form-group third">
              <label for="state">State</label>
              <input type="text" id="state" name="state" [(ngModel)]="model.state" required class="form-control" />
            </div>
            <div class="form-group third">
              <label for="pincode">Pincode</label>
              <input type="text" id="pincode" name="pincode" [(ngModel)]="model.pincode" required class="form-control" />
            </div>
          </div>

          <button type="submit" [disabled]="!regForm.form.valid || isLoading" class="btn-primary">
            <span *ngIf="!isLoading">Create Bank Account</span>
            <span *ngIf="isLoading">Opening Account...</span>
          </button>
        </form>

        <div class="auth-footer">
          <p>Already registered? <a routerLink="/login">Sign in</a></p>
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
      padding: 24px;
    }
    .auth-card {
      background: #ffffff;
      border-radius: 16px;
      padding: 36px;
      width: 100%;
      max-width: 540px;
      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2);
    }
    .brand {
      text-align: center;
      margin-bottom: 24px;
    }
    .brand .logo {
      font-size: 36px;
      margin-bottom: 6px;
    }
    .brand h2 {
      margin: 0;
      color: #0f172a;
      font-size: 24px;
      font-weight: 700;
    }
    .brand p {
      margin: 4px 0 0;
      color: #64748b;
      font-size: 13px;
    }
    .form-row {
      display: flex;
      gap: 12px;
    }
    .form-group {
      margin-bottom: 16px;
    }
    .form-group.half {
      flex: 1;
    }
    .form-group.third {
      flex: 1;
    }
    .form-group label {
      display: block;
      margin-bottom: 6px;
      font-size: 12px;
      font-weight: 600;
      color: #334155;
    }
    .form-control {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
      font-size: 13px;
      box-sizing: border-box;
    }
    .form-control:focus {
      outline: none;
      border-color: #2563eb;
    }
    .btn-primary {
      width: 100%;
      padding: 12px;
      background: #2563eb;
      color: #ffffff;
      border: none;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      margin-top: 8px;
    }
    .btn-primary:disabled {
      opacity: 0.6;
    }
    .alert.error {
      background: #fee2e2;
      color: #991b1b;
      padding: 10px;
      border-radius: 8px;
      font-size: 13px;
      margin-bottom: 16px;
    }
    .auth-footer {
      text-align: center;
      margin-top: 20px;
      font-size: 13px;
      color: #64748b;
    }
    .auth-footer a {
      color: #2563eb;
      text-decoration: none;
      font-weight: 600;
    }
  `]
})
export class RegisterComponent {
  model = {
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    password: '',
    address: '',
    city: '',
    state: '',
    pincode: ''
  };
  isLoading = false;
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  onRegister(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.authService.register(this.model).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.success) {
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Registration failed. Check inputs.';
      }
    });
  }
}
