package com.agentbanking.switchadapter.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record DuitNowRequest(
    UUID internalTransactionId,
    
    String idempotencyKey,
    
    @NotBlank(message = "proxyType is required")
    String proxyType,
    
    @NotBlank(message = "proxyValue is required")
    String proxyValue,
    
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    BigDecimal amount,
    
    String currency,
    
    String recipientName,
    
    String remark
) {
    public UUID getEffectiveTransactionId() {
        if (internalTransactionId != null) {
            return internalTransactionId;
        }
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return UUID.nameUUIDFromBytes(idempotencyKey.getBytes());
        }
        return UUID.randomUUID();
    }
}
