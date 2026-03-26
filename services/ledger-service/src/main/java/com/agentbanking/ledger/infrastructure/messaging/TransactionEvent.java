package com.agentbanking.ledger.infrastructure.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionEvent(
    UUID eventId,
    String status,
    UUID transactionId,
    UUID agentId,
    String transactionType,
    BigDecimal amount,
    String currency,
    String errorCode,
    String customerCardMasked
) {}
