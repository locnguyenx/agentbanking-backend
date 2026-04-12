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
    public StpDecision evaluateStp(Input input) {
        return rulesServicePort.evaluateStp(
            new RulesServicePort.StpEvaluationInput(
                input.transactionType(),
                UUID.fromString(input.agentId()),
                new BigDecimal(input.amount()),
                input.customerProfile(),
                input.agentTier(),
                input.transactionCountToday(),
                input.amountToday() != null ? new BigDecimal(input.amountToday()) : BigDecimal.ZERO,
                input.todayTotalAmount() != null ? new BigDecimal(input.todayTotalAmount()) : BigDecimal.ZERO
            )
        );
    }
}
