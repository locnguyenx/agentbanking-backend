package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "switch-adapter-qr", url = "${switch-adapter-service.url}")
public interface QRPaymentClient {

    @PostMapping("/internal/qr/generate")
    QRGenerationResponse generateQR(@RequestBody QRGenerationRequest request);

    @GetMapping("/internal/qr/status/{qrReference}")
    QRPaymentStatusResponse checkStatus(@PathVariable String qrReference);

    record QRGenerationRequest(BigDecimal amount, UUID agentId, String idempotencyKey) {}
    record QRGenerationResponse(String qrCode, String qrReference, String errorCode) {}
    record QRPaymentStatusResponse(String status, String paynetReference, String errorCode) {}
}
