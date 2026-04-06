package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.EvaluateStpActivity;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.StpDecision;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class EvaluateStpActivityImpl implements EvaluateStpActivity {

    private final RulesServicePort rulesServicePort;

    public EvaluateStpActivityImpl(RulesServicePort rulesServicePort) {
        this.rulesServicePort = rulesServicePort;
    }

    @Override
    public StpDecision evaluateStp(String transactionType, String agentId, String amount, String customerProfile) {
        return rulesServicePort.evaluateStp(
                transactionType,
                UUID.fromString(agentId),
                new BigDecimal(amount),
                customerProfile);
    }
}
