package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface BillerServicePort {

    BillValidationResult validateBill(BillValidationInput input);

    BillPaymentResult payBill(BillPaymentInput input);

    BillNotificationResult notifyBiller(BillNotificationInput input);

    BillNotificationResult notifyBillerReversal(BillReversalInput input);

    record BillValidationInput(
        String billerCode,
        String ref1
    ) {}

    record BillValidationResult(
        boolean valid,
        String accountName,
        BigDecimal amountDue,
        String errorCode
    ) {}

    record BillPaymentInput(
        String billerCode,
        String ref1,
        String ref2,
        BigDecimal amount,
        String idempotencyKey
    ) {}

    record BillPaymentResult(
        boolean success,
        String billerReference,
        String errorCode
    ) {}

    record BillNotificationInput(
        String internalTransactionId,
        BigDecimal amount
    ) {}

    record BillNotificationResult(
        boolean success,
        String errorCode
    ) {}

    record BillReversalInput(
        String billerCode,
        String ref1
    ) {}
}
