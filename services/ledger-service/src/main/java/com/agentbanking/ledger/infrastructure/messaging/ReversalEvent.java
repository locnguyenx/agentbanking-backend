package com.agentbanking.ledger.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReversalEvent(
    UUID eventId,
    String eventType,
    UUID originalTransactionId,
    UUID reversalTransactionId,
    UUID agentId,
    BigDecimal amount,
    String currency,
    String originalTransactionType,
    String reason,
    LocalDateTime timestamp
) {}
