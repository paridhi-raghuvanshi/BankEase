package com.bankease.api.exception;

import com.bankease.api.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception handling for all BankEase REST controllers.
 * All handlers return a consistent {@link ApiResponse} envelope with the
 * appropriate HTTP status code so that consumers always receive a predictable
 * JSON structure.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -------------------------------------------------------------------------
    // BankEase domain exceptions
    // -------------------------------------------------------------------------

    /**
     * Handles all custom {@link BankEaseException} subclasses, delegating the
     * HTTP status directly from the exception instance. This covers:
     * <ul>
     *   <li>{@link ResourceNotFoundException} → 404</li>
     *   <li>{@link InsufficientFundsException} → 422</li>
     *   <li>{@link AccountFrozenException} → 403</li>
     *   <li>{@link InvalidTransactionException} → 400</li>
     *   <li>{@link DuplicateBeneficiaryException} → 409</li>
     * </ul>
     */
    @ExceptionHandler(BankEaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBankEaseException(BankEaseException ex) {
        log.warn("BankEase domain exception [{}]: {}", ex.getStatus(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Validation exceptions
    // -------------------------------------------------------------------------

    /**
     * Handles bean-validation failures from {@code @Valid} / {@code @Validated}
     * on request bodies. Returns a 400 Bad Request with a map of field names to
     * their first validation error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Invalid value",
                        // keep the first error when the same field appears multiple times
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ));

        log.debug("Validation failed with {} field error(s): {}", fieldErrors.size(), fieldErrors);

        ApiResponse<Map<String, String>> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage("Validation failed. Please correct the highlighted fields.");
        response.setData(fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles constraint violations raised against method parameters
     * (e.g. path variables or query params annotated directly on the controller).
     * Returns a 400 Bad Request with a map of parameter names to violation messages.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
            ConstraintViolationException ex) {

        Map<String, String> violations = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        cv -> {
                            String path = cv.getPropertyPath().toString();
                            // Strip the method name prefix (e.g. "methodName.paramName" → "paramName")
                            int dotIndex = path.lastIndexOf('.');
                            return dotIndex >= 0 ? path.substring(dotIndex + 1) : path;
                        },
                        cv -> cv.getMessage() != null ? cv.getMessage() : "Invalid value",
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ));

        log.debug("Constraint violation(s): {}", violations);

        ApiResponse<Map<String, String>> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage("Constraint violation. Please review the request parameters.");
        response.setData(violations);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // -------------------------------------------------------------------------
    // Spring Security exceptions
    // -------------------------------------------------------------------------

    /**
     * Handles access-denied errors from Spring Security method security
     * (e.g. {@code @PreAuthorize} failures).
     * Returns a 403 Forbidden response.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You do not have permission to perform this action."));
    }

    /**
     * Handles authentication failures (invalid credentials, expired tokens, etc.).
     * Returns a 401 Unauthorised response.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failure: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication failed: " + ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Catch-all
    // -------------------------------------------------------------------------

    /**
     * Safety net handler for any unhandled {@link Exception}.
     * Logs the full stack trace and returns a generic 500 Internal Server Error
     * response so that internal details are never exposed to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later or contact support."));
    }
}
