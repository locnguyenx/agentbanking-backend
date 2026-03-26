package com.agentbanking.biller.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TopupTransactionRecord(
    UUID topupId,
    UUID internalTransactionId,
    String telco,
    String phoneNumber,
    BigDecimal amount,
    PaymentStatus status,
    String telcoReference,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {}