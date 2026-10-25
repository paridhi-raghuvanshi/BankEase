package com.bankease.api.service;

import com.bankease.api.dto.response.AccountResponse;
import com.bankease.api.entity.Account;
import com.bankease.api.exception.ResourceNotFoundException;
import com.bankease.api.repository.AccountRepository;
import com.bankease.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for account-related read operations.
 * <p>
 * All queries are scoped to a specific {@code userId} so that one user can never
 * read another user's account data.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    /**
     * Returns all accounts belonging to the specified user.
     *
     * @param userId the ID of the owning user
     * @return list of {@link AccountResponse} DTOs (may be empty)
     */
    public List<AccountResponse> getAccountsByUserId(Long userId) {
        log.debug("Fetching all accounts for userId={}", userId);
        assertUserExists(userId);
        return accountRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single account by its primary key, verifying ownership.
     *
     * @param id     the account primary key
     * @param userId the ID of the requesting user
     * @return the matching {@link AccountResponse}
     * @throws ResourceNotFoundException if no account with that ID belongs to the user
     */
    public AccountResponse getAccountById(Long id, Long userId) {
        log.debug("Fetching account id={} for userId={}", id, userId);
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + id));
        return mapToResponse(account);
    }

    /**
     * Returns a single account by its account number, verifying ownership.
     *
     * @param number the 16-digit account number string
     * @param userId the ID of the requesting user
     * @return the matching {@link AccountResponse}
     * @throws ResourceNotFoundException if no account with that number belongs to the user
     */
    public AccountResponse getAccountByNumber(String number, Long userId) {
        log.debug("Fetching account number={} for userId={}", number, userId);
        Account account = accountRepository.findByAccountNumberAndUserId(number, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with account number: " + number));
        return mapToResponse(account);
    }

    /**
     * Returns the current balance of an account identified by its account number.
     *
     * @param accountNumber the 16-digit account number string
     * @param userId        the ID of the requesting user
     * @return the current balance as {@link BigDecimal}
     * @throws ResourceNotFoundException if no account with that number belongs to the user
     */
    public BigDecimal getBalance(String accountNumber, Long userId) {
        log.debug("Fetching balance for accountNumber={}, userId={}", accountNumber, userId);
        Account account = accountRepository.findByAccountNumberAndUserId(accountNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with account number: " + accountNumber));
        return account.getBalance();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Asserts that a user with the given ID exists in the repository.
     *
     * @param userId the user ID to verify
     * @throws ResourceNotFoundException if the user does not exist
     */
    private void assertUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
    }

    /**
     * Manually maps an {@link Account} entity to an {@link AccountResponse} DTO.
     *
     * @param account the entity to map
     * @return populated response DTO
     */
    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType().name())
                .balance(account.getBalance())
                .availableBalance(account.getAvailableBalance())
                .currency(account.getCurrency())
                .status(account.getStatus().name())
                .ifscCode(account.getIfscCode())
                .branchName(account.getBranchName())
                .openedAt(account.getOpenedAt())
                .build();
    }
}
