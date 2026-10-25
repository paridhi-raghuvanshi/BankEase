package com.bankease.api.service;

import com.bankease.api.dto.request.FixedDepositRequest;
import com.bankease.api.dto.request.RecurringDepositRequest;
import com.bankease.api.dto.response.FixedDepositResponse;
import com.bankease.api.dto.response.RecurringDepositResponse;
import com.bankease.api.entity.*;
import com.bankease.api.exception.BankEaseException;
import com.bankease.api.exception.InsufficientFundsException;
import com.bankease.api.exception.ResourceNotFoundException;
import com.bankease.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentService {

    private final FixedDepositRepository fdRepository;
    private final RecurringDepositRepository rdRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public FixedDepositResponse createFixedDeposit(FixedDepositRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findByAccountNumberAndUserId(request.getAccountNumber(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (account.getBalance().compareTo(request.getPrincipalAmount()) < 0) {
            throw new InsufficientFundsException(account.getAccountNumber(), request.getPrincipalAmount(), account.getBalance());
        }

        // Debit source account
        account.setBalance(account.getBalance().subtract(request.getPrincipalAmount()));
        account.setAvailableBalance(account.getAvailableBalance().subtract(request.getPrincipalAmount()));
        accountRepository.save(account);

        BigDecimal rate = request.getInterestRate() != null ? request.getInterestRate() : new BigDecimal("7.10");
        BigDecimal maturityAmount = computeFdMaturity(request.getPrincipalAmount(), rate, request.getTenureMonths());

        LocalDate startDate = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(request.getTenureMonths());
        String fdNo = "FD" + System.currentTimeMillis() + (int)(Math.random() * 900 + 100);

        FixedDeposit fd = FixedDeposit.builder()
                .fdNumber(fdNo)
                .user(user)
                .account(account)
                .principalAmount(request.getPrincipalAmount())
                .interestRate(rate)
                .tenureMonths(request.getTenureMonths())
                .startDate(startDate)
                .maturityDate(maturityDate)
                .maturityAmount(maturityAmount)
                .interestPayoutType(request.getInterestPayoutType() != null ? request.getInterestPayoutType() : FixedDeposit.InterestPayoutType.ON_MATURITY)
                .status(FixedDeposit.FDStatus.ACTIVE)
                .build();

        fd = fdRepository.save(fd);

        // Record Debit Transaction
        Transaction txn = Transaction.builder()
                .referenceNumber("TXN" + System.currentTimeMillis())
                .type(Transaction.TransactionType.DEBIT)
                .mode(Transaction.TransactionMode.INTERNAL)
                .amount(request.getPrincipalAmount())
                .currency("INR")
                .description("Opened Fixed Deposit: " + fdNo)
                .status(Transaction.TransactionStatus.SUCCESS)
                .sourceAccount(account)
                .balanceAfterTransaction(account.getBalance())
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(txn);

        auditLogService.log(userId, user.getEmail(), "CREATE_FD", "Created FD: " + fdNo, null);
        return mapToFdResponse(fd);
    }

    @Transactional(readOnly = true)
    public List<FixedDepositResponse> getFixedDeposits(Long userId) {
        return fdRepository.findByUserId(userId).stream()
                .map(this::mapToFdResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FixedDepositResponse getFixedDepositById(Long id, Long userId) {
        FixedDeposit fd = fdRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed deposit not found"));
        return mapToFdResponse(fd);
    }

    @Transactional
    public RecurringDepositResponse createRecurringDeposit(RecurringDepositRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findByAccountNumberAndUserId(request.getAccountNumber(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (account.getBalance().compareTo(request.getMonthlyInstallment()) < 0) {
            throw new InsufficientFundsException(account.getAccountNumber(), request.getMonthlyInstallment(), account.getBalance());
        }

        // Debit initial installment
        account.setBalance(account.getBalance().subtract(request.getMonthlyInstallment()));
        account.setAvailableBalance(account.getAvailableBalance().subtract(request.getMonthlyInstallment()));
        accountRepository.save(account);

        BigDecimal rate = request.getInterestRate() != null ? request.getInterestRate() : new BigDecimal("6.90");
        BigDecimal maturityAmount = computeRdMaturity(request.getMonthlyInstallment(), rate, request.getTenureMonths());

        LocalDate startDate = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(request.getTenureMonths());
        String rdNo = "RD" + System.currentTimeMillis() + (int)(Math.random() * 900 + 100);

        RecurringDeposit rd = RecurringDeposit.builder()
                .rdNumber(rdNo)
                .user(user)
                .account(account)
                .monthlyInstallment(request.getMonthlyInstallment())
                .interestRate(rate)
                .tenureMonths(request.getTenureMonths())
                .installmentsPaid(1)
                .startDate(startDate)
                .maturityDate(maturityDate)
                .nextInstallmentDate(startDate.plusMonths(1))
                .totalDeposited(request.getMonthlyInstallment())
                .maturityAmount(maturityAmount)
                .status(RecurringDeposit.RDStatus.ACTIVE)
                .build();

        rd = rdRepository.save(rd);

        Transaction txn = Transaction.builder()
                .referenceNumber("TXN" + System.currentTimeMillis())
                .type(Transaction.TransactionType.DEBIT)
                .mode(Transaction.TransactionMode.RD_INSTALLMENT)
                .amount(request.getMonthlyInstallment())
                .currency("INR")
                .description("Opened Recurring Deposit: " + rdNo)
                .status(Transaction.TransactionStatus.SUCCESS)
                .sourceAccount(account)
                .balanceAfterTransaction(account.getBalance())
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(txn);

        auditLogService.log(userId, user.getEmail(), "CREATE_RD", "Created RD: " + rdNo, null);
        return mapToRdResponse(rd);
    }

    @Transactional(readOnly = true)
    public List<RecurringDepositResponse> getRecurringDeposits(Long userId) {
        return rdRepository.findByUserId(userId).stream()
                .map(this::mapToRdResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecurringDepositResponse getRecurringDepositById(Long id, Long userId) {
        RecurringDeposit rd = rdRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring deposit not found"));
        return mapToRdResponse(rd);
    }

    private BigDecimal computeFdMaturity(BigDecimal p, BigDecimal r, int months) {
        double principal = p.doubleValue();
        double annualRate = r.doubleValue() / 100.0;
        double quarters = months / 3.0;
        double amount = principal * Math.pow(1 + (annualRate / 4.0), quarters);
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeRdMaturity(BigDecimal installment, BigDecimal r, int months) {
        double P = installment.doubleValue();
        double i = (r.doubleValue() / 100.0) / 12.0;
        double maturity = 0;
        for (int m = 1; m <= months; m++) {
            maturity += P * Math.pow(1 + i, m);
        }
        return BigDecimal.valueOf(maturity).setScale(2, RoundingMode.HALF_UP);
    }

    private FixedDepositResponse mapToFdResponse(FixedDeposit fd) {
        return FixedDepositResponse.builder()
                .id(fd.getId())
                .fdNumber(fd.getFdNumber())
                .principalAmount(fd.getPrincipalAmount())
                .interestRate(fd.getInterestRate())
                .tenureMonths(fd.getTenureMonths())
                .startDate(fd.getStartDate())
                .maturityDate(fd.getMaturityDate())
                .maturityAmount(fd.getMaturityAmount())
                .interestPayoutType(fd.getInterestPayoutType() != null ? fd.getInterestPayoutType().name() : null)
                .status(fd.getStatus().name())
                .build();
    }

    private RecurringDepositResponse mapToRdResponse(RecurringDeposit rd) {
        return RecurringDepositResponse.builder()
                .id(rd.getId())
                .rdNumber(rd.getRdNumber())
                .monthlyInstallment(rd.getMonthlyInstallment())
                .interestRate(rd.getInterestRate())
                .tenureMonths(rd.getTenureMonths())
                .installmentsPaid(rd.getInstallmentsPaid())
                .totalDeposited(rd.getTotalDeposited())
                .maturityAmount(rd.getMaturityAmount())
                .maturityDate(rd.getMaturityDate())
                .nextInstallmentDate(rd.getNextInstallmentDate())
                .status(rd.getStatus().name())
                .build();
    }
}
