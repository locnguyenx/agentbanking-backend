package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "essp-service", url = "${essp-service.url}")
public interface ESSPServiceClient {

    @PostMapping("/internal/validate-purchase")
    ESSPValidationResponse validatePurchase(@RequestBody ESSPValidationRequest request);

    @PostMapping("/internal/purchase")
    ESSPPurchaseResponse purchase(@RequestBody ESSPPurchaseRequest request);

    record ESSPValidationRequest(BigDecimal amount) {}
    record ESSPValidationResponse(boolean valid, BigDecimal minAmount, BigDecimal maxAmount, String errorCode) {}
    record ESSPPurchaseRequest(BigDecimal amount, String customerMykad, String idempotencyKey) {}
    record ESSPPurchaseResponse(boolean success, String certificateNumber, String errorCode) {}
}
