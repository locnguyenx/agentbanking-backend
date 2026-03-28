package com.agentbanking.biller.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Use case for processing eSSP certificate purchase
 */
public interface ProcessEsspUseCase {
    EsspTransactionResult processEsspPurchase(EsspTransactionCommand command);

    record EsspTransactionCommand(
        BigDecimal amount,
        UUID internalTransactionId
    ) {}

    record EsspTransactionResult(
        String status,
        String transactionId,
        String esspCertificateNumber
    ) {}
}