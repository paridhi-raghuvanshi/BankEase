package com.bankease.api.dto.request;

import com.bankease.api.entity.Biller.BillerCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for registering a biller (utility, subscription, etc.) for a user.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddBillerRequest {

    @NotBlank(message = "Biller name is required")
    @Size(max = 100, message = "Biller name must not exceed 100 characters")
    private String billerName;

    @NotNull(message = "Biller category is required")
    private BillerCategory category;

    @NotBlank(message = "Consumer ID is required")
    @Size(max = 50, message = "Consumer ID must not exceed 50 characters")
    private String consumerId;

    @NotBlank(message = "Consumer name is required")
    @Size(max = 100, message = "Consumer name must not exceed 100 characters")
    private String consumerName;

    @NotBlank(message = "Nickname is required")
    @Size(min = 2, max = 50, message = "Nickname must be between 2 and 50 characters")
    private String nickname;
}
