package com.agentbanking.ledger.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Use case for processing cash-back transactions
 */
public interface ProcessCashBackUseCase {
    CashBackResponse processCashBack(CashBackCommand command);

    record CashBackCommand(
        UUID merchantId,
        BigDecimal cashBackAmount,
        String cardData,
        String pinBlock,
        String idempotencyKey
    ) {}

    record CashBackResponse(
        String status,
        String transactionId,
        BigDecimal cashBackAmount,
        BigDecimal commission
    ) {}
}
