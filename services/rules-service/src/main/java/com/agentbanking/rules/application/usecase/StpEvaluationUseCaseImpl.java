package com.agentbanking.rules.application.usecase;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.model.StpDecision;
import com.agentbanking.rules.domain.port.in.StpEvaluationUseCase;
import com.agentbanking.rules.domain.service.StpDecisionService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StpEvaluationUseCaseImpl implements StpEvaluationUseCase {

    private final StpDecisionService stpDecisionService;

    public StpEvaluationUseCaseImpl(StpDecisionService stpDecisionService) {
        this.stpDecisionService = stpDecisionService;
    }

    @Override
    public StpEvaluationResponse evaluate(StpEvaluationCommand command) {
        AgentTier tier;
        try {
            if (command.agentTier() == null || command.agentTier().isBlank()) {
                tier = AgentTier.STANDARD;
            } else {
                tier = AgentTier.valueOf(command.agentTier().toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            tier = AgentTier.MICRO;
        }

        StpDecision decision = stpDecisionService.evaluate(
            command.transactionType(),
            command.customerMykad(),
            command.amount(),
            tier,
            command.transactionCountToday(),
            command.amountToday(),
            command.feeConfig(),
            command.todayTotalAmount()
        );

        return new StpEvaluationResponse(
            decision.category(),
            decision.approved(),
            decision.reason(),
            decision.velocityRemaining(),
            decision.limitRemaining()
        );
    }
}
