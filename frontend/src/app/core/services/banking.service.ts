import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiResponse,
  Account,
  Transaction,
  Beneficiary,
  Biller,
  BillPayment,
  Loan,
  FixedDeposit,
  RecurringDeposit,
  PagedResponse
} from '../models/banking.models';

@Injectable({
  providedIn: 'root'
})
export class BankingService {
  private baseAccountsUrl = '/api/v1/accounts';
  private baseTransfersUrl = '/api/v1/transfers';
  private baseBeneficiariesUrl = '/api/v1/beneficiaries';
  private baseBillsUrl = '/api/v1/bills';
  private baseLoansUrl = '/api/v1/loans';
  private baseInvestmentsUrl = '/api/v1/investments';

  constructor(private http: HttpClient) {}

  // Account Operations
  getAccounts(): Observable<ApiResponse<Account[]>> {
    return this.http.get<ApiResponse<Account[]>>(this.baseAccountsUrl);
  }

  getAccountByNumber(accountNumber: string): Observable<ApiResponse<Account>> {
    return this.http.get<ApiResponse<Account>>(`${this.baseAccountsUrl}/${accountNumber}`);
  }

  getAccountStatement(accountNumber: string, page = 0, size = 10): Observable<ApiResponse<PagedResponse<Transaction>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PagedResponse<Transaction>>>(`${this.baseAccountsUrl}/${accountNumber}/statement`, { params });
  }

  // Transfer Operations
  transferFunds(transferData: {
    sourceAccountNumber: string;
    destinationAccountNumber: string;
    amount: number;
    mode: 'NEFT' | 'RTGS' | 'IMPS';
    description?: string;
  }): Observable<ApiResponse<Transaction>> {
    return this.http.post<ApiResponse<Transaction>>(this.baseTransfersUrl, transferData);
  }

  getTransferHistory(): Observable<ApiResponse<PagedResponse<Transaction>>> {
    return this.http.get<ApiResponse<PagedResponse<Transaction>>>(`${this.baseTransfersUrl}/history`);
  }

  // Beneficiary Operations
  getBeneficiaries(): Observable<ApiResponse<Beneficiary[]>> {
    return this.http.get<ApiResponse<Beneficiary[]>>(this.baseBeneficiariesUrl);
  }

  addBeneficiary(beneficiary: {
    nickname: string;
    accountHolderName: string;
    accountNumber: string;
    ifscCode: string;
    bankName: string;
    branchName: string;
  }): Observable<ApiResponse<Beneficiary>> {
    return this.http.post<ApiResponse<Beneficiary>>(this.baseBeneficiariesUrl, beneficiary);
  }

  deleteBeneficiary(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseBeneficiariesUrl}/${id}`);
  }

  // Bill Pay Operations
  getBillers(): Observable<ApiResponse<Biller[]>> {
    return this.http.get<ApiResponse<Biller[]>>(`${this.baseBillsUrl}/billers`);
  }

  addBiller(biller: {
    billerName: string;
    category: string;
    consumerId: string;
    consumerName?: string;
    nickname?: string;
  }): Observable<ApiResponse<Biller>> {
    return this.http.post<ApiResponse<Biller>>(`${this.baseBillsUrl}/billers`, biller);
  }

  payBill(payment: {
    billerId: number;
    accountNumber: string;
    amount: number;
    billPeriod: string;
  }): Observable<ApiResponse<BillPayment>> {
    return this.http.post<ApiResponse<BillPayment>>(`${this.baseBillsUrl}/pay`, payment);
  }

  getBillPaymentHistory(): Observable<ApiResponse<PagedResponse<BillPayment>>> {
    return this.http.get<ApiResponse<PagedResponse<BillPayment>>>(`${this.baseBillsUrl}/history`);
  }

  // Loan Operations
  getLoans(): Observable<ApiResponse<Loan[]>> {
    return this.http.get<ApiResponse<Loan[]>>(this.baseLoansUrl);
  }

  applyForLoan(loanData: {
    accountNumber: string;
    loanType: string;
    principalAmount: number;
    tenureMonths: number;
    purpose?: string;
  }): Observable<ApiResponse<Loan>> {
    return this.http.post<ApiResponse<Loan>>(`${this.baseLoansUrl}/apply`, loanData);
  }

  calculateEmi(principal: number, rate: number, tenureMonths: number): Observable<ApiResponse<any>> {
    const params = new HttpParams()
      .set('principal', principal)
      .set('rate', rate)
      .set('tenureMonths', tenureMonths);
    return this.http.get<ApiResponse<any>>(`${this.baseLoansUrl}/calculate-emi`, { params });
  }

  // Investment Operations
  getFixedDeposits(): Observable<ApiResponse<FixedDeposit[]>> {
    return this.http.get<ApiResponse<FixedDeposit[]>>(`${this.baseInvestmentsUrl}/fd`);
  }

  createFixedDeposit(fdData: {
    accountNumber: string;
    principalAmount: number;
    tenureMonths: number;
    interestPayoutType?: string;
  }): Observable<ApiResponse<FixedDeposit>> {
    return this.http.post<ApiResponse<FixedDeposit>>(`${this.baseInvestmentsUrl}/fd`, fdData);
  }

  getRecurringDeposits(): Observable<ApiResponse<RecurringDeposit[]>> {
    return this.http.get<ApiResponse<RecurringDeposit[]>>(`${this.baseInvestmentsUrl}/rd`);
  }

  createRecurringDeposit(rdData: {
    accountNumber: string;
    monthlyInstallment: number;
    tenureMonths: number;
  }): Observable<ApiResponse<RecurringDeposit>> {
    return this.http.post<ApiResponse<RecurringDeposit>>(`${this.baseInvestmentsUrl}/rd`, rdData);
  }
}
