package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.ESSPServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class ESSPServiceAdapter implements ESSPServicePort {

    private static final Logger log = LoggerFactory.getLogger(ESSPServiceAdapter.class);

    private final ESSPServiceClient client;

    public ESSPServiceAdapter(ESSPServiceClient client) {
        this.client = client;
    }

    @Override
    public ESSPValidationResult validatePurchase(BigDecimal amount) {
        log.info("Validating eSSP purchase amount: {}", amount);
        var response = client.validatePurchase(new ESSPServiceClient.ESSPValidationRequest(amount));
        return new ESSPValidationResult(response.valid(), response.minAmount(), response.maxAmount(), response.errorCode());
    }

    @Override
    public ESSPPurchaseResult purchase(BigDecimal amount, String customerMykad, String idempotencyKey) {
        log.info("Processing eSSP purchase: {}", amount);
        var response = client.purchase(new ESSPServiceClient.ESSPPurchaseRequest(amount, customerMykad, idempotencyKey));
        return new ESSPPurchaseResult(response.success(), response.certificateNumber(), response.errorCode());
    }
}
