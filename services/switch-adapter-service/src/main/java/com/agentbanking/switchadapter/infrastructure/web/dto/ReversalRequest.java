package com.agentbanking.switchadapter.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record ReversalRequest(
    @NotNull(message = "originalTransactionId is required")
    UUID originalTransactionId,
    
    @NotBlank(message = "originalReference is required")
    String originalReference,
    
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    BigDecimal amount
) {}
