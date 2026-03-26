package com.agentbanking.biller.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface PayBillUseCase {
    PayBillResult payBill(String billerCode, String ref1, BigDecimal amount, UUID internalTransactionId);

    record PayBillResult(
        UUID paymentId,
        String status,
        String receiptNo,
        String billerReference,
        BigDecimal amount
    ) {}
}