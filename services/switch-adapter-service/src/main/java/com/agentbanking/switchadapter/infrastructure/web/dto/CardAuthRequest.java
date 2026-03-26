package com.agentbanking.switchadapter.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CardAuthRequest(
    @NotNull(message = "internalTransactionId is required")
    UUID internalTransactionId,
    
    @NotBlank(message = "pan is required")
    String pan,
    
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    BigDecimal amount
) {}
