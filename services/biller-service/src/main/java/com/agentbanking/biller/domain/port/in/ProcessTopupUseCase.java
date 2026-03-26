package com.agentbanking.biller.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProcessTopupUseCase {
    ProcessTopupResult processTopup(String telco, String phoneNumber, BigDecimal amount, UUID internalTransactionId);

    record ProcessTopupResult(
        UUID topupId,
        String status,
        String telcoReference,
        BigDecimal amount
    ) {}
}