package com.agentbanking.orchestrator.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ESSPServiceClientFallbackFactory implements FallbackFactory<ESSPServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(ESSPServiceClientFallbackFactory.class);

    @Override
    public ESSPServiceClient create(Throwable cause) {
        log.error("ESSPServiceClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new ESSPServiceClient() {
            @Override
            public ESSPServiceClient.ESSPValidationResponse validatePurchase(ESSPServiceClient.ESSPValidationRequest request) {
                log.warn("ESSP service unavailable, auto-validating purchase for amount: {}", request.amount());
                return new ESSPServiceClient.ESSPValidationResponse(true, BigDecimal.valueOf(10), BigDecimal.valueOf(1000), "ESSP_UNAVAILABLE");
            }

            @Override
            public ESSPServiceClient.ESSPPurchaseResponse purchase(ESSPServiceClient.ESSPPurchaseRequest request) {
                log.warn("ESSP service unavailable, auto-approving purchase for amount: {}", request.amount());
                return new ESSPServiceClient.ESSPPurchaseResponse(true, "ESSP_CERT_" + System.currentTimeMillis(), "ESSP_UNAVAILABLE");
            }
        };
    }
}