package com.agentbanking.ledger.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Use case for processing card-based deposits
 */
public interface ProcessCardDepositUseCase {
    TransactionResult processCardDeposit(CardDepositCommand command);

    record CardDepositCommand(
        UUID agentId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String customerCardData,
        String customerPinBlock
    ) {}

    record TransactionResult(
        String status,
        UUID transactionId,
        BigDecimal amount,
        BigDecimal customerFee,
        String referenceNumber
    ) {}
}