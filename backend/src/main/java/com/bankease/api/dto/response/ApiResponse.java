package com.bankease.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper used by all BankEase endpoints.
 *
 * @param <T> the type of the payload data
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Creates a successful response containing only data (no custom message).
     *
     * @param data the response payload
     * @param <T>  the type of the payload
     * @return a successful {@link ApiResponse}
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data);
    }

    /**
     * Creates a successful response with a custom message and data.
     *
     * @param message a descriptive success message
     * @param data    the response payload
     * @param <T>     the type of the payload
     * @return a successful {@link ApiResponse}
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * Creates an error response with no data payload.
     *
     * @param message a descriptive error message
     * @param <T>     the expected (absent) payload type
     * @return an error {@link ApiResponse}
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
