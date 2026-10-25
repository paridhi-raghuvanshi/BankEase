export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface UserSummary {
  id: number;
  customerId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  roles: string[];
  status: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export interface Account {
  id: number;
  accountNumber: string;
  ifscCode: string;
  accountType: 'SAVINGS' | 'CURRENT' | 'SALARY' | 'NRI';
  balance: number;
  availableBalance: number;
  currency: string;
  status: 'ACTIVE' | 'INACTIVE' | 'FROZEN' | 'CLOSED';
  dailyTransferLimit: number;
  createdAt: string;
}

export interface Transaction {
  id: number;
  referenceNumber: string;
  type: 'DEBIT' | 'CREDIT';
  mode: 'NEFT' | 'RTGS' | 'IMPS' | 'UPI' | 'INTERNAL' | 'BILL_PAYMENT' | 'LOAN_EMI' | 'FD_INTEREST' | 'RD_INSTALLMENT' | 'CHARGE';
  amount: number;
  charges: number;
  currency: string;
  description: string;
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'REVERSED';
  beneficiaryName?: string;
  beneficiaryAccountNumber?: string;
  beneficiaryIfsc?: string;
  balanceAfterTransaction: number;
  createdAt: string;
  processedAt?: string;
}

export interface Beneficiary {
  id: number;
  nickname: string;
  accountHolderName: string;
  accountNumber: string;
  ifscCode: string;
  bankName: string;
  branchName: string;
  status: string;
  verified: boolean;
  createdAt: string;
}

export interface Biller {
  id: number;
  billerName: string;
  category: string;
  consumerId: string;
  consumerName?: string;
  nickname?: string;
  dueAmount?: number;
  dueDate?: string;
  status: string;
}

export interface BillPayment {
  id: number;
  billerName: string;
  amount: number;
  billPeriod: string;
  confirmationNumber: string;
  status: string;
  paidAt: string;
}

export interface Loan {
  id: number;
  loanNumber: string;
  loanType: 'HOME' | 'PERSONAL' | 'CAR' | 'EDUCATION' | 'GOLD' | 'BUSINESS';
  principalAmount: number;
  outstandingAmount: number;
  interestRate: number;
  tenureMonths: number;
  emiAmount: number;
  startDate: string;
  endDate: string;
  nextEmiDate: string;
  status: string;
  purpose?: string;
}

export interface FixedDeposit {
  id: number;
  fdNumber: string;
  principalAmount: number;
  interestRate: number;
  tenureMonths: number;
  startDate: string;
  maturityDate: string;
  maturityAmount: number;
  interestPayoutType: string;
  status: string;
}

export interface RecurringDeposit {
  id: number;
  rdNumber: string;
  monthlyInstallment: number;
  interestRate: number;
  tenureMonths: number;
  installmentsPaid: number;
  totalDeposited: number;
  maturityAmount: number;
  maturityDate: string;
  nextInstallmentDate: string;
  status: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
