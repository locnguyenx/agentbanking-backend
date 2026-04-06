package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDate;

@FeignClient(name = "pin-inventory", url = "${pin-inventory.url}")
public interface PINInventoryClient {

    @PostMapping("/internal/validate-inventory")
    PINValidationResponse validateInventory(@RequestBody PINValidationRequest request);

    @PostMapping("/internal/generate-pin")
    PINGenerationResponse generatePIN(@RequestBody PINGenerationRequest request);

    record PINValidationRequest(String provider, BigDecimal faceValue) {}
    record PINValidationResponse(boolean available, int stockCount, String errorCode) {}
    record PINGenerationRequest(String provider, BigDecimal faceValue, String idempotencyKey) {}
    record PINGenerationResponse(boolean success, String pinCode, String serialNumber, LocalDate expiryDate, String errorCode) {}
}
