package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.StpDecision;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface EvaluateStpActivity {
    StpDecision evaluateStp(String transactionType, String agentId, String amount, String customerProfile);
}
