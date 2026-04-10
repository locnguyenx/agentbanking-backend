package com.agentbanking.ledger.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Use case for processing retail sale transactions
 */
public interface ProcessRetailSaleUseCase {
    RetailSaleResponse processRetailSale(RetailSaleCommand command);

    record RetailSaleCommand(
        UUID merchantId,
        BigDecimal amount,
        String cardData,
        String pinBlock,
        String idempotencyKey,
        UUID agentId,
        String description,
        String referenceNumber,
        String agentTier,
        String targetBin,
        String billerCode,
        String destinationAccount,
        String ref1,
        String ref2
    ) {}

    record RetailSaleResponse(
        String status,
        String transactionId,
        BigDecimal amount,
        BigDecimal mdrAmount,
        BigDecimal netToMerchant
    ) {}
}