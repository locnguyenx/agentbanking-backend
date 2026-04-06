package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.RulesServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class RulesServiceAdapter implements RulesServicePort {

    private static final Logger log = LoggerFactory.getLogger(RulesServiceAdapter.class);

    private final RulesServiceClient rulesServiceClient;

    public RulesServiceAdapter(RulesServiceClient rulesServiceClient) {
        this.rulesServiceClient = rulesServiceClient;
    }

    @Override
    public VelocityCheckResult checkVelocity(VelocityCheckInput input) {
        log.info("Checking velocity for agent: {}", input.agentId());
        return rulesServiceClient.checkVelocity(input);
    }

    @Override
    public FeeCalculationResult calculateFees(FeeCalculationInput input) {
        log.info("Calculating fees for type: {}, tier: {}", input.transactionType(), input.agentTier());
        return rulesServiceClient.calculateFees(input);
    }

    @Override
    public StpDecision evaluateStp(String transactionType, UUID agentId, BigDecimal amount, String customerProfile) {
        log.info("Evaluating STP for type: {}, agent: {}, amount: {}", transactionType, agentId, amount);
        return rulesServiceClient.evaluateStp(transactionType, agentId, amount, customerProfile);
    }
}
