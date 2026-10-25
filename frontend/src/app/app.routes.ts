import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login.component';
import { RegisterComponent } from './features/auth/register.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { TransferComponent } from './features/transfer/transfer.component';
import { BillsComponent } from './features/bills/bills.component';
import { LoansComponent } from './features/loans/loans.component';
import { InvestmentsComponent } from './features/investments/investments.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'transfer', component: TransferComponent, canActivate: [authGuard] },
  { path: 'bills', component: BillsComponent, canActivate: [authGuard] },
  { path: 'loans', component: LoansComponent, canActivate: [authGuard] },
  { path: 'investments', component: InvestmentsComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'dashboard' }
];
