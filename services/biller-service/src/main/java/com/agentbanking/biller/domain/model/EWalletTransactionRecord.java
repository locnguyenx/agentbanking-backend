package com.agentbanking.biller.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Record representing an e-Wallet transaction (Sarawak Pay withdrawal/top-up)
 */
public record EWalletTransactionRecord(
    UUID transactionId,
    UUID internalTransactionId,
    String walletProvider, // e.g., "SARAWAK_PAY"
    String walletId,       // Customer's wallet ID
    BigDecimal amount,
    PaymentStatus status,
    String walletReference,
    String agentReference,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {}