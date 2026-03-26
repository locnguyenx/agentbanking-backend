package com.agentbanking.switchadapter.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProcessReversalUseCase {
    ProcessReversalResult processReversal(UUID originalTransactionId, String originalReference, BigDecimal amount);

    record ProcessReversalResult(
        UUID switchTxId,
        String status,
        String responseCode,
        String referenceId
    ) {}
}