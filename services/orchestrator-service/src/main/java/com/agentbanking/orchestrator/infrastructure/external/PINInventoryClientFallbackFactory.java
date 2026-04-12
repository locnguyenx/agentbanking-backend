package com.agentbanking.orchestrator.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class PINInventoryClientFallbackFactory implements FallbackFactory<PINInventoryClient> {

    private static final Logger log = LoggerFactory.getLogger(PINInventoryClientFallbackFactory.class);

    @Override
    public PINInventoryClient create(Throwable cause) {
        log.error("PINInventoryClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new PINInventoryClient() {
            @Override
            public PINInventoryClient.PINValidationResponse validateInventory(PINInventoryClient.PINValidationRequest request) {
                log.warn("PIN inventory service unavailable, auto-approving PIN validation for provider: {}", request.provider());
                return new PINInventoryClient.PINValidationResponse(true, 100, "PIN_INVENTORY_UNAVAILABLE");
            }

            @Override
            public PINInventoryClient.PINGenerationResponse generatePIN(PINInventoryClient.PINGenerationRequest request) {
                log.warn("PIN inventory service unavailable, generating test PIN for provider: {}", request.provider());
                return new PINInventoryClient.PINGenerationResponse(
                    true, 
                    "1234567890123456", 
                    "TEST_PIN_" + System.currentTimeMillis(), 
                    LocalDate.now().plusDays(30), 
                    "PIN_INVENTORY_UNAVAILABLE"
                );
            }
        };
    }
}