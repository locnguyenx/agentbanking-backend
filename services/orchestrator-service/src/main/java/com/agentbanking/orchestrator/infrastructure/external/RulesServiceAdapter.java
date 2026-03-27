package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.RulesServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RulesServiceAdapter implements RulesServicePort {

    private static final Logger log = LoggerFactory.getLogger(RulesServiceAdapter.class);

    private final RulesServiceClient rulesServiceClient;

    public RulesServiceAdapter(RulesServiceClient rulesServiceClient) {
        this.rulesServiceClient = rulesServiceClient;
    }

    @Override
    public Map<String, Object> checkVelocity(Map<String, Object> request) {
        log.info("Checking velocity for agent: {}", request.get("agentId"));
        return rulesServiceClient.checkVelocity(request);
    }

    @Override
    public Map<String, Object> calculateFees(Map<String, Object> request) {
        log.info("Calculating fees for agent: {}", request.get("agentId"));
        return rulesServiceClient.calculateFees(request);
    }
}
