package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface ESSPServicePort {

    ESSPValidationResult validatePurchase(BigDecimal amount);

    ESSPPurchaseResult purchase(BigDecimal amount, String customerMykad, String idempotencyKey);

    record ESSPValidationResult(boolean valid, BigDecimal minAmount, BigDecimal maxAmount, String errorCode) {}
    record ESSPPurchaseResult(boolean success, String certificateNumber, String errorCode) {}
}
