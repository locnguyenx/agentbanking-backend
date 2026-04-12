package com.agentbanking.orchestrator.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TelcoAggregatorClientFallbackFactory implements FallbackFactory<TelcoAggregatorClient> {

    private static final Logger log = LoggerFactory.getLogger(TelcoAggregatorClientFallbackFactory.class);

    @Override
    public TelcoAggregatorClient create(Throwable cause) {
        log.error("TelcoAggregatorClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new TelcoAggregatorClient() {
            @Override
            public TelcoAggregatorClient.TelcoPhoneValidationResponse validatePhone(TelcoAggregatorClient.TelcoPhoneValidationRequest request) {
                log.warn("Telco aggregator service unavailable, auto-validating phone: {} for provider: {}", request.phoneNumber(), request.telcoProvider());
                return new TelcoAggregatorClient.TelcoPhoneValidationResponse(true, "DIGI", "TELCO_UNAVAILABLE");
            }

            @Override
            public TelcoAggregatorClient.TelcoTopupResponse topup(TelcoAggregatorClient.TelcoTopupRequest request) {
                log.warn("Telco aggregator service unavailable, auto-approving topup for phone: {} via {}", request.phoneNumber(), request.telcoProvider());
                return new TelcoAggregatorClient.TelcoTopupResponse(true, "TELCO_REF_" + System.currentTimeMillis(), "TELCO_UNAVAILABLE");
            }
        };
    }
}