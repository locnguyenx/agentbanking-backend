package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.PINInventoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class PINInventoryAdapter implements PINInventoryPort {

    private static final Logger log = LoggerFactory.getLogger(PINInventoryAdapter.class);

    private final PINInventoryClient client;

    public PINInventoryAdapter(PINInventoryClient client) {
        this.client = client;
    }

    @Override
    public PINInventoryResult validateInventory(String provider, BigDecimal faceValue) {
        log.info("Validating PIN inventory for {}: {}", provider, faceValue);
        var response = client.validateInventory(new PINInventoryClient.PINValidationRequest(provider, faceValue));
        return new PINInventoryResult(response.available(), response.stockCount(), response.errorCode());
    }

    @Override
    public PINGenerationResult generatePIN(String provider, BigDecimal faceValue, String idempotencyKey) {
        log.info("Generating PIN for {}: {}", provider, faceValue);
        var response = client.generatePIN(new PINInventoryClient.PINGenerationRequest(provider, faceValue, idempotencyKey));
        return new PINGenerationResult(response.success(), response.pinCode(), response.serialNumber(), response.expiryDate(), response.errorCode());
    }
}
