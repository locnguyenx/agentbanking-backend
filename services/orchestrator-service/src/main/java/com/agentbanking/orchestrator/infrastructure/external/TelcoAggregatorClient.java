package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "telco-aggregator", url = "${telco-aggregator.url}", fallbackFactory = TelcoAggregatorClientFallbackFactory.class)
public interface TelcoAggregatorClient {

    @PostMapping("/internal/validate-phone")
    TelcoPhoneValidationResponse validatePhone(@RequestBody TelcoPhoneValidationRequest request);

    @PostMapping("/internal/topup")
    TelcoTopupResponse topup(@RequestBody TelcoTopupRequest request);

    record TelcoPhoneValidationRequest(String phoneNumber, String telcoProvider) {}
    record TelcoPhoneValidationResponse(boolean valid, String operatorName, String errorCode) {}
    record TelcoTopupRequest(String telcoProvider, String phoneNumber, BigDecimal amount, String idempotencyKey) {}
    record TelcoTopupResponse(boolean success, String telcoReference, String errorCode) {}
}
