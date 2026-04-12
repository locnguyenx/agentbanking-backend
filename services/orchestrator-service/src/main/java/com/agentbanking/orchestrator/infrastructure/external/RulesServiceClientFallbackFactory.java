package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class RulesServiceClientFallbackFactory implements FallbackFactory<RulesServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(RulesServiceClientFallbackFactory.class);

    @Override
    public RulesServiceClient create(Throwable cause) {
        log.error("RulesServiceClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new RulesServiceClient() {
            @Override
            public VelocityCheckResult checkVelocity(VelocityCheckInput input) {
                log.warn("Rules service unavailable, allowing velocity check for agent: {}", input.agentId());
                return new VelocityCheckResult(true, null);
            }

            @Override
            public FeeCalculationResult calculateFees(FeeCalculationInput input) {
                log.warn("Rules service unavailable, using default fees for transaction: {}", input.transactionType());
                // Default fees: 2.00 customer fee, 0.50 agent commission, 1.50 bank share
                return new FeeCalculationResult(
                    BigDecimal.valueOf(2.00),
                    BigDecimal.valueOf(0.50),
                    BigDecimal.valueOf(1.50)
                );
            }

            @Override
            public StpDecision evaluateStp(RulesServiceClient.StpEvaluationRequest request) {
                log.warn("Rules service unavailable, defaulting to STP approval");
                return new StpDecision("STP", true, "Auto-approved - rules service unavailable");
            }
        };
    }
}