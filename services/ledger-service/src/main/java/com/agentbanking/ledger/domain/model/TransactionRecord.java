package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionRecord(
    UUID transactionId,
    String idempotencyKey,
    UUID agentId,
    TransactionType transactionType,
    BigDecimal amount,
    BigDecimal customerFee,
    BigDecimal agentCommission,
    BigDecimal bankShare,
    TransactionStatus status,
    String errorCode,
    String customerMykad,
    String customerCardMasked,
    String switchReference,
    String referenceNumber,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng,
    LocalDateTime createdAt,
    LocalDateTime completedAt,
    String agentTier,
    String targetBin,
    String billerCode,
    String ref1,
    String ref2,
    String destinationAccount
) {}
