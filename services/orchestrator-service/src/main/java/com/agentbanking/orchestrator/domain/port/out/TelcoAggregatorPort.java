package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface TelcoAggregatorPort {

    TelcoValidationResult validatePhoneNumber(String phoneNumber, String telcoProvider);

    TelcoTopupResult processTopup(String telcoProvider, String phoneNumber, BigDecimal amount, String idempotencyKey);

    record TelcoValidationResult(boolean valid, String operatorName, String errorCode) {}
    record TelcoTopupResult(boolean success, String telcoReference, String errorCode) {}
}
