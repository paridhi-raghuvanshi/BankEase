package com.bankease.api.service;

import com.bankease.api.dto.request.FundTransferRequest;
import com.bankease.api.dto.response.PagedResponse;
import com.bankease.api.dto.response.TransactionResponse;
import com.bankease.api.entity.Account;
import com.bankease.api.entity.Transaction;
import com.bankease.api.exception.InsufficientFundsException;
import com.bankease.api.exception.ResourceNotFoundException;
import com.bankease.api.exception.TransactionException;
import com.bankease.api.repository.AccountRepository;
import com.bankease.api.repository.BeneficiaryRepository;
import com.bankease.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for fund transfers, transaction history, and reference lookups.
 * <p>
 * All state-changing operations are wrapped in a single database transaction so
 * that partial debits or credits can never occur. Charge logic follows RBI
 * prescribed fee schedules for RTGS, NEFT, and IMPS.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    // Charge constants
    private static final BigDecimal RTGS_RATE          = new BigDecimal("0.0025");   // 0.25 %
    private static final BigDecimal RTGS_MIN_CHARGE    = new BigDecimal("25.00");
    private static final BigDecimal NEFT_FLAT_CHARGE   = new BigDecimal("2.50");
    private static final BigDecimal IMPS_RATE          = new BigDecimal("0.005");    // 0.50 %
    private static final BigDecimal IMPS_MIN_CHARGE    = new BigDecimal("5.00");
    private static final BigDecimal IMPS_MAX_CHARGE    = new BigDecimal("15.00");

    // Daily transfer limit per account
    private static final BigDecimal DAILY_TRANSFER_LIMIT = new BigDecimal("200000.00");

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    // -------------------------------------------------------------------------
    // Fund Transfer
    // -------------------------------------------------------------------------

    /**
     * Transfers funds from a source account owned by the caller to a destination account.
     * <ol>
     *   <li>Validates the source account ownership and {@code ACTIVE} status.</li>
     *   <li>Checks that sufficient funds are available (balance – charge).</li>
     *   <li>Enforces the daily transfer limit across all outgoing transactions.</li>
     *   <li>Calculates mode-specific service charges.</li>
     *   <li>Debits the source account and credits the destination account within the same transaction.</li>
     *   <li>Creates two {@link Transaction} records (DEBIT and CREDIT legs).</li>
     * </ol>
     *
     * @param request the transfer request DTO
     * @param userId  the ID of the authenticated user initiating the transfer
     * @return a {@link TransactionResponse} representing the debit leg of the transfer
     * @throws ResourceNotFoundException  if the source account does not belong to the user
     * @throws TransactionException       if the source account is not active
     * @throws InsufficientFundsException if the balance is insufficient to cover amount + charges
     * @throws TransactionException       if the daily transfer limit would be breached
     */
    @Transactional
    public TransactionResponse transferFunds(FundTransferRequest request, Long userId) {
        log.info("Fund transfer initiated: userId={}, from={}, to={}, amount={}",
                userId, request.getSourceAccountNumber(),
                request.getDestinationAccountNumber(), request.getAmount());

        // 1. Load and validate source account
        Account source = accountRepository
                .findByAccountNumberAndUserId(request.getSourceAccountNumber(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Source account not found or does not belong to you: "
                                + request.getSourceAccountNumber()));

        if (source.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new TransactionException(
                    "Source account " + source.getAccountNumber() + " is not active. Status: "
                            + source.getStatus());
        }

        // 2. Load destination account (can belong to any user)
        Account destination = accountRepository
                .findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destination account not found: " + request.getDestinationAccountNumber()));

        if (destination.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new TransactionException(
                    "Destination account " + destination.getAccountNumber()
                            + " is not active. Status: " + destination.getStatus());
        }

        BigDecimal amount = request.getAmount();

        // 3. Calculate service charge
        BigDecimal charge = calculateCharge(request.getTransferMode(), amount);
        BigDecimal totalDebit = amount.add(charge);

        // 4. Sufficient balance check
        if (source.getBalance().compareTo(totalDebit) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient balance. Required: " + totalDebit
                            + " (amount " + amount + " + charge " + charge + ")"
                            + ", Available: " + source.getBalance());
        }

        // 5. Daily limit check
        BigDecimal todayTotal = transactionRepository
                .sumDebitAmountByAccountAndDate(source.getId(), LocalDate.now());
        if (todayTotal == null) todayTotal = BigDecimal.ZERO;

        if (todayTotal.add(amount).compareTo(DAILY_TRANSFER_LIMIT) > 0) {
            throw new TransactionException(
                    "Daily transfer limit of " + DAILY_TRANSFER_LIMIT
                            + " would be exceeded. Already transferred today: " + todayTotal);
        }

        // 6. Perform debit / credit (with pessimistic lock to prevent race conditions)
        Account lockedSource = accountRepository.findByIdWithLock(source.getId());
        lockedSource.setBalance(lockedSource.getBalance().subtract(totalDebit));
        lockedSource.setAvailableBalance(lockedSource.getAvailableBalance().subtract(totalDebit));
        accountRepository.save(lockedSource);

        destination.setBalance(destination.getBalance().add(amount));
        destination.setAvailableBalance(destination.getAvailableBalance().add(amount));
        accountRepository.save(destination);

        // 7. Create transaction records
        String referenceNumber = generateReferenceNumber();
        LocalDateTime now = LocalDateTime.now();

        Transaction debitTxn = Transaction.builder()
                .account(lockedSource)
                .type(Transaction.TransactionType.DEBIT)
                .amount(totalDebit)
                .netAmount(amount)
                .charge(charge)
                .description(request.getDescription() != null
                        ? request.getDescription()
                        : "Fund transfer to " + destination.getAccountNumber())
                .transferMode(request.getTransferMode())
                .referenceNumber(referenceNumber)
                .counterpartAccountNumber(destination.getAccountNumber())
                .balanceAfter(lockedSource.getBalance())
                .status(Transaction.TransactionStatus.SUCCESS)
                .initiatedAt(now)
                .completedAt(now)
                .build();

        Transaction creditTxn = Transaction.builder()
                .account(destination)
                .type(Transaction.TransactionType.CREDIT)
                .amount(amount)
                .netAmount(amount)
                .charge(BigDecimal.ZERO)
                .description("Fund received from " + lockedSource.getAccountNumber())
                .transferMode(request.getTransferMode())
                .referenceNumber(referenceNumber)
                .counterpartAccountNumber(lockedSource.getAccountNumber())
                .balanceAfter(destination.getBalance())
                .status(Transaction.TransactionStatus.SUCCESS)
                .initiatedAt(now)
                .completedAt(now)
                .build();

        Transaction savedDebit = transactionRepository.save(debitTxn);
        transactionRepository.save(creditTxn);

        log.info("Transfer complete: ref={}, amount={}, charge={}", referenceNumber, amount, charge);
        return mapToResponse(savedDebit);
    }

    // -------------------------------------------------------------------------
    // Transaction History
    // -------------------------------------------------------------------------

    /**
     * Returns a paginated history of transactions for an account owned by the given user.
     *
     * @param accountId the account primary key
     * @param userId    the ID of the requesting user
     * @param page      zero-based page index
     * @param size      maximum records per page
     * @return paginated list of {@link TransactionResponse} DTOs
     */
    public PagedResponse<TransactionResponse> getTransactionHistory(
            Long accountId, Long userId, int page, int size) {
        log.debug("Fetching transaction history: accountId={}, userId={}, page={}, size={}",
                accountId, userId, page, size);

        assertAccountOwnership(accountId, userId);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "initiatedAt"));

        Page<Transaction> txnPage = transactionRepository
                .findByAccountId(accountId, pageable);

        return buildPagedResponse(txnPage);
    }

    /**
     * Returns transactions for an account filtered by a date range.
     *
     * @param accountId the account primary key
     * @param userId    the ID of the requesting user
     * @param from      start date (inclusive)
     * @param to        end date (inclusive)
     * @param page      zero-based page index
     * @param size      maximum records per page
     * @return paginated list of {@link TransactionResponse} DTOs within the date range
     */
    public PagedResponse<TransactionResponse> getTransactionsByDateRange(
            Long accountId, Long userId,
            LocalDate from, LocalDate to,
            int page, int size) {
        log.debug("Fetching transactions by date range: accountId={}, from={}, to={}", accountId, from, to);

        assertAccountOwnership(accountId, userId);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "initiatedAt"));

        Page<Transaction> txnPage = transactionRepository
                .findByAccountIdAndInitiatedAtBetween(
                        accountId,
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay(),
                        pageable);

        return buildPagedResponse(txnPage);
    }

    /**
     * Retrieves a transaction by its reference number, scoped to the requesting user.
     *
     * @param ref    the reference number (e.g. {@code TXN20241025123456})
     * @param userId the ID of the requesting user
     * @return the matching {@link TransactionResponse}
     * @throws ResourceNotFoundException if the transaction is not found or does not belong to the user
     */
    public TransactionResponse getTransactionByRef(String ref, Long userId) {
        log.debug("Fetching transaction by ref={} for userId={}", ref, userId);
        Transaction txn = transactionRepository
                .findByReferenceNumberAndAccount_UserId(ref, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with reference: " + ref));
        return mapToResponse(txn);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Calculates the service charge based on the transfer mode and amount.
     *
     * <ul>
     *   <li><b>RTGS</b>: 0.25% of amount, minimum ₹25</li>
     *   <li><b>NEFT</b>: flat ₹2.50</li>
     *   <li><b>IMPS</b>: 0.50% of amount, minimum ₹5, maximum ₹15</li>
     * </ul>
     *
     * @param mode   the transfer mode string (case-insensitive)
     * @param amount the principal transfer amount
     * @return the calculated charge rounded to 2 decimal places
     */
    private BigDecimal calculateCharge(String mode, BigDecimal amount) {
        return switch (mode.toUpperCase()) {
            case "RTGS" -> {
                BigDecimal charge = amount.multiply(RTGS_RATE)
                        .setScale(2, RoundingMode.HALF_UP);
                yield charge.compareTo(RTGS_MIN_CHARGE) < 0 ? RTGS_MIN_CHARGE : charge;
            }
            case "NEFT" -> NEFT_FLAT_CHARGE;
            case "IMPS" -> {
                BigDecimal charge = amount.multiply(IMPS_RATE)
                        .setScale(2, RoundingMode.HALF_UP);
                if (charge.compareTo(IMPS_MIN_CHARGE) < 0) {
                    yield IMPS_MIN_CHARGE;
                } else if (charge.compareTo(IMPS_MAX_CHARGE) > 0) {
                    yield IMPS_MAX_CHARGE;
                } else {
                    yield charge;
                }
            }
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * Generates a unique transaction reference number in the format
     * {@code TXN<yyyyMMddHHmmss><6-random-digits>}.
     *
     * @return a 23-character reference number string
     */
    private String generateReferenceNumber() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int suffix = 100_000 + secureRandom.nextInt(900_000);
        return "TXN" + timestamp + suffix;
    }

    /**
     * Verifies that the given account belongs to the given user.
     *
     * @param accountId the account ID to check
     * @param userId    the expected owner
     * @throws ResourceNotFoundException if the account does not belong to the user
     */
    private void assertAccountOwnership(Long accountId, Long userId) {
        if (!accountRepository.existsByIdAndUserId(accountId, userId)) {
            throw new ResourceNotFoundException(
                    "Account not found with id: " + accountId);
        }
    }

    /**
     * Builds a {@link PagedResponse} from a Spring Data {@link Page} of transactions.
     *
     * @param txnPage the raw page result from the repository
     * @return the populated paged response DTO
     */
    private PagedResponse<TransactionResponse> buildPagedResponse(Page<Transaction> txnPage) {
        List<TransactionResponse> content = txnPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<TransactionResponse>builder()
                .content(content)
                .page(txnPage.getNumber())
                .size(txnPage.getSize())
                .totalElements(txnPage.getTotalElements())
                .totalPages(txnPage.getTotalPages())
                .last(txnPage.isLast())
                .build();
    }

    /**
     * Maps a {@link Transaction} entity to its response DTO.
     *
     * @param t the entity to map
     * @return populated {@link TransactionResponse}
     */
    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .accountId(t.getAccount().getId())
                .accountNumber(t.getAccount().getAccountNumber())
                .type(t.getType().name())
                .amount(t.getAmount())
                .netAmount(t.getNetAmount())
                .charge(t.getCharge())
                .description(t.getDescription())
                .transferMode(t.getTransferMode())
                .referenceNumber(t.getReferenceNumber())
                .counterpartAccountNumber(t.getCounterpartAccountNumber())
                .balanceAfter(t.getBalanceAfter())
                .status(t.getStatus().name())
                .initiatedAt(t.getInitiatedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }
}
