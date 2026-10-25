package com.bankease.api.service;

import com.bankease.api.dto.request.LoanApplicationRequest;
import com.bankease.api.dto.response.LoanResponse;
import com.bankease.api.entity.Account;
import com.bankease.api.entity.Loan;
import com.bankease.api.entity.User;
import com.bankease.api.exception.BankEaseException;
import com.bankease.api.exception.ResourceNotFoundException;
import com.bankease.api.repository.AccountRepository;
import com.bankease.api.repository.LoanRepository;
import com.bankease.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public LoanResponse applyForLoan(LoanApplicationRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findByAccountNumberAndUserId(request.getAccountNumber(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        BigDecimal rate = getStandardInterestRate(request.getLoanType());
        BigDecimal emi = computeEmi(request.getPrincipalAmount(), rate, request.getTenureMonths());

        String loanNo = "LN" + System.currentTimeMillis() + (int)(Math.random() * 900 + 100);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(request.getTenureMonths());

        Loan loan = Loan.builder()
                .loanNumber(loanNo)
                .user(user)
                .account(account)
                .loanType(request.getLoanType())
                .principalAmount(request.getPrincipalAmount())
                .outstandingAmount(request.getPrincipalAmount())
                .interestRate(rate)
                .tenureMonths(request.getTenureMonths())
                .emiAmount(emi)
                .startDate(startDate)
                .endDate(endDate)
                .nextEmiDate(startDate.plusMonths(1))
                .status(Loan.LoanStatus.APPLIED)
                .purpose(request.getPurpose())
                .build();

        loan = loanRepository.save(loan);
        auditLogService.log(userId, user.getEmail(), "LOAN_APPLICATION", "Applied for loan: " + loanNo, null);
        return mapToLoanResponse(loan);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getLoans(Long userId) {
        return loanRepository.findByUserId(userId)
                .stream()
                .map(this::mapToLoanResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanById(Long id, Long userId) {
        Loan loan = loanRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        return mapToLoanResponse(loan);
    }

    public Map<String, Object> calculateEmi(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        BigDecimal emi = computeEmi(principal, annualRate, tenureMonths);
        BigDecimal totalPayment = emi.multiply(BigDecimal.valueOf(tenureMonths));
        BigDecimal totalInterest = totalPayment.subtract(principal);

        Map<String, Object> response = new HashMap<>();
        response.put("principalAmount", principal);
        response.put("interestRate", annualRate);
        response.put("tenureMonths", tenureMonths);
        response.put("emiAmount", emi);
        response.put("totalPayment", totalPayment.setScale(2, RoundingMode.HALF_UP));
        response.put("totalInterest", totalInterest.setScale(2, RoundingMode.HALF_UP));
        return response;
    }

    private BigDecimal computeEmi(BigDecimal p, BigDecimal rAnnual, int n) {
        if (rAnnual.compareTo(BigDecimal.ZERO) == 0) {
            return p.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        }
        double annualRate = rAnnual.doubleValue();
        double monthlyRate = annualRate / (12 * 100);
        double emi = (p.doubleValue() * monthlyRate * Math.pow(1 + monthlyRate, n))
                / (Math.pow(1 + monthlyRate, n) - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getStandardInterestRate(Loan.LoanType loanType) {
        return switch (loanType) {
            case HOME -> new BigDecimal("8.50");
            case PERSONAL -> new BigDecimal("11.50");
            case CAR -> new BigDecimal("9.00");
            case EDUCATION -> new BigDecimal("8.00");
            case GOLD -> new BigDecimal("7.50");
            case BUSINESS -> new BigDecimal("12.00");
        };
    }

    private LoanResponse mapToLoanResponse(Loan loan) {
        return LoanResponse.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .loanType(loan.getLoanType())
                .principalAmount(loan.getPrincipalAmount())
                .outstandingAmount(loan.getOutstandingAmount())
                .interestRate(loan.getInterestRate())
                .tenureMonths(loan.getTenureMonths())
                .emiAmount(loan.getEmiAmount())
                .startDate(loan.getStartDate())
                .endDate(loan.getEndDate())
                .nextEmiDate(loan.getNextEmiDate())
                .status(loan.getStatus().name())
                .purpose(loan.getPurpose())
                .build();
    }
}
