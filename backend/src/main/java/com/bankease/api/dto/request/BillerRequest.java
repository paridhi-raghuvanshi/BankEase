package com.bankease.api.dto.request;

import com.bankease.api.entity.Biller.BillerCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillerRequest {
    @NotBlank
    private String billerName;

    @NotNull
    private BillerCategory category;

    @NotBlank
    private String consumerId;

    private String consumerName;

    private String nickname;
}
