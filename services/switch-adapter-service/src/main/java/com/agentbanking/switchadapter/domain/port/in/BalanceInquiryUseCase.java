package com.agentbanking.switchadapter.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface BalanceInquiryUseCase {
    BalanceInquiryResult inquiryBalance(UUID internalTransactionId, String encryptedCardData, String pinBlock);

    record BalanceInquiryResult(
        UUID switchTxId,
        String status,
        String responseCode,
        String referenceId,
        BigDecimal balance,
        String currency,
        String accountMasked
    ) {}
}
