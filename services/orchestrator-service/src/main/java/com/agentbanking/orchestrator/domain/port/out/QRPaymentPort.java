package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface QRPaymentPort {

    QRGenerationResult generateDynamicQR(BigDecimal amount, UUID agentId, String idempotencyKey);

    QRPaymentStatus checkPaymentStatus(String qrReference);

    record QRGenerationResult(String qrCode, String qrReference, String errorCode) {}
    record QRPaymentStatus(String status, String paynetReference, String errorCode) {}
}
