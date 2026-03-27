package com.agentbanking.orchestrator.domain.port.out;

import java.util.Map;

public interface RulesServicePort {
    Map<String, Object> checkVelocity(Map<String, Object> request);
    Map<String, Object> calculateFees(Map<String, Object> request);
}
