package com.agentbanking.biller.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BillPaymentRecord(
    UUID paymentId,
    UUID billerId,
    UUID internalTransactionId,
    String ref1,
    String ref2,
    BigDecimal amount,
    PaymentStatus status,
    String receiptNo,
    String billerReference,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {}