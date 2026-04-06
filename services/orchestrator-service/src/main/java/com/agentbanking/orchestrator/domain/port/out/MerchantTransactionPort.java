package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface MerchantTransactionPort {

    MerchantTransactionResult createRecord(MerchantTransactionRecord record);

    record MerchantTransactionRecord(
        UUID transactionId,
        String merchantType,
        BigDecimal grossAmount,
        BigDecimal mdrRate,
        BigDecimal mdrAmount,
        BigDecimal netCreditToFloat,
        String receiptType
    ) {}

    record MerchantTransactionResult(boolean success, UUID recordId, String errorCode) {}
}
