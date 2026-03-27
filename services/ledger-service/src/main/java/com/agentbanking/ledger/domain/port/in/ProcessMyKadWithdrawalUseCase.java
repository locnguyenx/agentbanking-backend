package com.agentbanking.ledger.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Use case for processing MyKad-based withdrawals
 */
public interface ProcessMyKadWithdrawalUseCase {
    TransactionResult processMyKadWithdrawal(MyKadWithdrawalCommand command);

    record MyKadWithdrawalCommand(
        UUID agentId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String customerMykad,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng
    ) {}

    record TransactionResult(
        String status,
        UUID transactionId,
        BigDecimal amount,
        BigDecimal customerFee,
        String referenceNumber
    ) {}
}