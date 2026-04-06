package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.QRPaymentPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public class QRPaymentAdapter implements QRPaymentPort {

    private static final Logger log = LoggerFactory.getLogger(QRPaymentAdapter.class);

    private final QRPaymentClient client;

    public QRPaymentAdapter(QRPaymentClient client) {
        this.client = client;
    }

    @Override
    public QRGenerationResult generateDynamicQR(BigDecimal amount, UUID agentId, String idempotencyKey) {
        log.info("Generating dynamic QR for agent {}: {}", agentId, amount);
        var response = client.generateQR(new QRPaymentClient.QRGenerationRequest(amount, agentId, idempotencyKey));
        return new QRGenerationResult(response.qrCode(), response.qrReference(), response.errorCode());
    }

    @Override
    public QRPaymentStatus checkPaymentStatus(String qrReference) {
        log.info("Checking QR payment status: {}", qrReference);
        var response = client.checkStatus(qrReference);
        return new QRPaymentStatus(response.status(), response.paynetReference(), response.errorCode());
    }
}
