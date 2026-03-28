package com.agentbanking.rules.domain.port.in;

import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.model.StpCategory;
import com.agentbanking.rules.domain.model.StpDecision;

import java.math.BigDecimal;

public interface StpEvaluationUseCase {
    StpEvaluationResponse evaluate(StpEvaluationCommand command);

    record StpEvaluationCommand(
        String transactionType,
        String customerMykad,
        BigDecimal amount,
        String agentTier,
        int transactionCountToday,
        BigDecimal amountToday,
        FeeConfigRecord feeConfig,
        BigDecimal todayTotalAmount
    ) {}

    record StpEvaluationResponse(
        StpCategory category,
        boolean approved,
        String reason,
        BigDecimal velocityRemaining,
        BigDecimal limitRemaining
    ) {}
}
