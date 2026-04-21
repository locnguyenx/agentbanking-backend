package com.agentbanking.switchadapter.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface DuitNowTransferUseCase {
    DuitNowTransferResult transferDuitNow(UUID internalTransactionId, String proxyType, String proxyValue, BigDecimal amount);

    record DuitNowTransferResult(
        UUID switchTxId,
        String status,
        String responseCode,
        String reference
    ) {}
}