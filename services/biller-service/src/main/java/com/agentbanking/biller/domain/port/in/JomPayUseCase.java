package com.agentbanking.biller.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface JomPayUseCase {
    JomPayResult processJomPay(JomPayCommand command);

    record JomPayCommand(
        String billerCode,
        String billerName,
        String ref1,
        String ref2,
        BigDecimal amount,
        String currency,
        UUID internalTransactionId
    ) {}

    record JomPayResult(
        UUID paymentId,
        String status,
        String receiptNo,
        String billerReference,
        BigDecimal amount
    ) {}
}
