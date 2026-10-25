package com.bankease.api.service;

import com.bankease.api.dto.request.BillPayRequest;
import com.bankease.api.dto.request.AddBillerRequest;
import com.bankease.api.dto.response.BillerResponse;
import com.bankease.api.dto.response.BillPaymentResponse;
import com.bankease.api.dto.response.PagedResponse;
import com.bankease.api.entity.*;
import com.bankease.api.exception.BankEaseException;
import com.bankease.api.exception.InsufficientFundsException;
import com.bankease.api.exception.ResourceNotFoundException;
import com.bankease.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillPayService {

    private final BillerRepository billerRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<BillerResponse> getBillers(Long userId) {
        return billerRepository.findByUserIdAndStatus(userId, Biller.BillerStatus.ACTIVE)
                .stream()
                .map(this::mapToBillerResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BillerResponse addBiller(AddBillerRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Biller biller = Biller.builder()
                .user(user)
                .billerName(request.getBillerName())
                .category(request.getCategory())
                .consumerId(request.getConsumerId())
                .consumerName(request.getConsumerName())
                .nickname(request.getNickname())
                .status(Biller.BillerStatus.ACTIVE)
                .build();

        biller = billerRepository.save(biller);
        auditLogService.log(userId, user.getEmail(), "ADD_BILLER", "Added biller: " + biller.getBillerName(), null);
        return mapToBillerResponse(biller);
    }

    @Transactional
    public void deleteBiller(Long id, Long userId) {
        Biller biller = billerRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Biller not found"));
        biller.setStatus(Biller.BillerStatus.DELETED);
        billerRepository.save(biller);
    }

    @Transactional
    public BillPaymentResponse payBill(BillPayRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Biller biller = billerRepository.findByIdAndUserId(request.getBillerId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Biller not found"));

        Account account = accountRepository.findByAccountNumberAndUserId(request.getAccountNumber(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BankEaseException("Account is not active", HttpStatus.BAD_REQUEST);
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(account.getAccountNumber(), request.getAmount(), account.getBalance());
        }

        // Debit account
        account.setBalance(account.getBalance().subtract(request.getAmount()));
        account.setAvailableBalance(account.getAvailableBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        String refNo = "TXN" + System.currentTimeMillis() + (int)(Math.random() * 900000 + 100000);
        Transaction txn = Transaction.builder()
                .referenceNumber(refNo)
                .type(Transaction.TransactionType.DEBIT)
                .mode(Transaction.TransactionMode.BILL_PAYMENT)
                .amount(request.getAmount())
                .currency("INR")
                .description("Bill Payment - " + biller.getBillerName() + " (" + request.getBillPeriod() + ")")
                .status(Transaction.TransactionStatus.SUCCESS)
                .sourceAccount(account)
                .balanceAfterTransaction(account.getBalance())
                .processedAt(LocalDateTime.now())
                .build();
        txn = transactionRepository.save(txn);

        String confirmationNo = "CONF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BillPayment billPayment = BillPayment.builder()
                .biller(biller)
                .account(account)
                .transaction(txn)
                .amount(request.getAmount())
                .billPeriod(request.getBillPeriod())
                .confirmationNumber(confirmationNo)
                .status(BillPayment.PaymentStatus.SUCCESS)
                .build();
        billPayment = billPaymentRepository.save(billPayment);

        auditLogService.log(userId, user.getEmail(), "PAY_BILL", "Paid bill: " + biller.getBillerName() + ", Amount: " + request.getAmount(), null);

        return BillPaymentResponse.builder()
                .id(billPayment.getId())
                .billerName(biller.getBillerName())
                .amount(billPayment.getAmount())
                .billPeriod(billPayment.getBillPeriod())
                .confirmationNumber(billPayment.getConfirmationNumber())
                .status(billPayment.getStatus().name())
                .paidAt(billPayment.getPaidAt())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<BillPaymentResponse> getPaymentHistory(Long userId, int page, int size) {
        Page<BillPayment> pageResult = billPaymentRepository.findByUserId(userId, PageRequest.of(page, size));
        List<BillPaymentResponse> responses = pageResult.getContent().stream()
                .map(bp -> BillPaymentResponse.builder()
                        .id(bp.getId())
                        .billerName(bp.getBiller().getBillerName())
                        .amount(bp.getAmount())
                        .billPeriod(bp.getBillPeriod())
                        .confirmationNumber(bp.getConfirmationNumber())
                        .status(bp.getStatus().name())
                        .paidAt(bp.getPaidAt())
                        .build())
                .collect(Collectors.toList());

        return PagedResponse.<BillPaymentResponse>builder()
                .content(responses)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    private BillerResponse mapToBillerResponse(Biller b) {
        return BillerResponse.builder()
                .id(b.getId())
                .billerName(b.getBillerName())
                .category(b.getCategory())
                .consumerId(b.getConsumerId())
                .consumerName(b.getConsumerName())
                .nickname(b.getNickname())
                .dueAmount(b.getDueAmount())
                .dueDate(b.getDueDate())
                .status(b.getStatus().name())
                .build();
    }
}
