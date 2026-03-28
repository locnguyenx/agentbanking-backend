package com.agentbanking.biller.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Record representing an eSSP certificate transaction
 */
public record EsspTransactionRecord(
    UUID transactionId,
    UUID internalTransactionId,
    BigDecimal amount,
    PaymentStatus status,
    String esspCertificateNumber,
    String agentReference,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {}