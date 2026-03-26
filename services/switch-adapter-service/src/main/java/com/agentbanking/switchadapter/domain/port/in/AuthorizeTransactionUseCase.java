package com.agentbanking.switchadapter.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface AuthorizeTransactionUseCase {
    AuthorizeTransactionResult authorizeTransaction(UUID internalTransactionId, String pan, BigDecimal amount);

    record AuthorizeTransactionResult(
        UUID switchTxId,
        String status,
        String responseCode,
        String referenceId
    ) {}
}