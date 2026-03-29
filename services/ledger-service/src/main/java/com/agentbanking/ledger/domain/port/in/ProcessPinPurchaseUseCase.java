package com.agentbanking.ledger.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Use case for processing PIN voucher purchases
 */
public interface ProcessPinPurchaseUseCase {
    PinPurchaseResponse processPinPurchase(PinPurchaseCommand command);

    record PinPurchaseCommand(
        UUID agentId,
        String productCode,
        BigDecimal amount,
        String idempotencyKey
    ) {}

    record PinPurchaseResponse(
        String status,
        String transactionId,
        String pinCode,
        BigDecimal commission,
        String timestamp
    ) {}
}
