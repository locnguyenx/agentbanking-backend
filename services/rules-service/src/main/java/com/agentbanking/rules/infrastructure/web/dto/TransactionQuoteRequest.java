package com.agentbanking.rules.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TransactionQuoteRequest(
    @NotNull(message = "amount is required")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "amount must be a valid decimal string")
    String amount,

    @NotBlank(message = "serviceCode is required")
    String serviceCode,

    @NotBlank(message = "fundingSource is required")
    String fundingSource,

    String billerRouting
) {}
