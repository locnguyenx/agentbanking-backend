package com.agentbanking.switchadapter.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SwitchTransactionRecord(
    UUID switchTxId,
    UUID internalTransactionId,
    MessageType mtType,
    String isoResponseCode,
    String switchReference,
    SwitchStatus status,
    String originalReference,
    int reversalCount,
    BigDecimal amount,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {}
