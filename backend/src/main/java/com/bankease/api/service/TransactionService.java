package com.bankease.api.service;

import com.bankease.api.dto.request.FundTransferRequest;
import com.bankease.api.dto.response.PagedResponse;
import com.bankease.api.dto.response.TransactionResponse;
import com.bankease.api.entity.Account;
import com.bankease.api.entity.Transaction;
import com.bankease.api.exception.BankEaseException;
import com.bankease.api.exception.InsufficientFundsException;
import com.bankease.api.exception.ResourceNotFoundException;
import com.bankease.api.repository.AccountRepository;
import com.bankease.api.repository.BeneficiaryRepository;
import com.bankease.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public TransactionResponse transferFunds(FundTransferRequest request, Long userId) {
        Account source = accountRepository.findByAccountNumberAndUserId(request.getSourceAccountNumber(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));

        if (source.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BankEaseException("Source account is not active", HttpStatus.BAD_REQUEST);
        }

        Transaction.TransactionMode mode = Transaction.TransactionMode.valueOf(request.getMode().name());
        BigDecimal charges = calculateCharges(mode, request.getAmount());
        BigDecimal totalRequired = request.getAmount().add(charges);

        if (source.getBalance().compareTo(totalRequired) < 0) {
            throw new InsufficientFundsException(source.getAccountNumber(), totalRequired, source.getBalance());
        }

        // Debit source account
        source.setBalance(source.getBalance().subtract(totalRequired));
        source.setAvailableBalance(source.getAvailableBalance().subtract(totalRequired));
        accountRepository.save(source);

        // Credit destination account if internal
        accountRepository.findByAccountNumber(request.getDestinationAccountNumber()).ifPresent(dest -> {
            dest.setBalance(dest.getBalance().add(request.getAmount()));
            dest.setAvailableBalance(dest.getAvailableBalance().add(request.getAmount()));
            accountRepository.save(dest);
        });

        String refNo = "TXN" + System.currentTimeMillis() + (int)(Math.random() * 900000 + 100000);
        Transaction txn = Transaction.builder()
                .referenceNumber(refNo)
                .type(Transaction.TransactionType.DEBIT)
                .mode(mode)
                .amount(request.getAmount())
                .charges(charges)
                .currency("INR")
                .description(request.getDescription())
                .status(Transaction.TransactionStatus.SUCCESS)
                .sourceAccount(source)
                .beneficiaryAccountNumber(request.getDestinationAccountNumber())
                .balanceAfterTransaction(source.getBalance())
                .processedAt(LocalDateTime.now())
                .build();

        txn = transactionRepository.save(txn);
        auditLogService.log(userId, source.getUser().getEmail(), "TRANSFER", "Transfer ref: " + refNo + " Amount: " + request.getAmount(), null);

        return mapToResponse(txn);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactionHistory(Long accountId, Long userId, int page, int size) {
        Page<Transaction> pageResult = transactionRepository.findByAccountId(accountId, PageRequest.of(page, size));
        return buildPagedResponse(pageResult);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactionsByDateRange(Long accountId, Long userId, LocalDate from, LocalDate to, int page, int size) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        Page<Transaction> pageResult = transactionRepository.findByAccountIdAndDateRange(accountId, start, end, PageRequest.of(page, size));
        return buildPagedResponse(pageResult);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByRef(String ref, Long userId) {
        Transaction txn = transactionRepository.findByReferenceNumber(ref)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return mapToResponse(txn);
    }

    private BigDecimal calculateCharges(Transaction.TransactionMode mode, BigDecimal amount) {
        if (mode == Transaction.TransactionMode.NEFT) {
            return new BigDecimal("2.50");
        } else if (mode == Transaction.TransactionMode.RTGS) {
            BigDecimal charge = amount.multiply(new BigDecimal("0.0025"));
            return charge.max(new BigDecimal("25.00"));
        } else if (mode == Transaction.TransactionMode.IMPS) {
            BigDecimal charge = amount.multiply(new BigDecimal("0.005"));
            return charge.max(new BigDecimal("5.00")).min(new BigDecimal("15.00"));
        }
        return BigDecimal.ZERO;
    }

    private PagedResponse<TransactionResponse> buildPagedResponse(Page<Transaction> pageResult) {
        List<TransactionResponse> list = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<TransactionResponse>builder()
                .content(list)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .referenceNumber(t.getReferenceNumber())
                .type(t.getType().name())
                .mode(t.getMode().name())
                .amount(t.getAmount())
                .charges(t.getCharges())
                .currency(t.getCurrency())
                .description(t.getDescription())
                .status(t.getStatus().name())
                .beneficiaryName(t.getBeneficiaryName())
                .beneficiaryAccountNumber(t.getBeneficiaryAccountNumber())
                .beneficiaryIfsc(t.getBeneficiaryIfsc())
                .balanceAfterTransaction(t.getBalanceAfterTransaction())
                .createdAt(t.getCreatedAt())
                .processedAt(t.getProcessedAt())
                .build();
    }
}
