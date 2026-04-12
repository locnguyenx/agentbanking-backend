package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface RulesServicePort {

    VelocityCheckResult checkVelocity(VelocityCheckInput input);

    FeeCalculationResult calculateFees(FeeCalculationInput input);

    record VelocityCheckInput(
        UUID agentId,
        BigDecimal amount,
        String customerMykad
    ) {}

    record VelocityCheckResult(
        boolean passed,
        String errorCode
    ) {}

    record FeeCalculationInput(
        String transactionType,
        String agentTier,
        BigDecimal amount
    ) {}

    record FeeCalculationResult(
        BigDecimal customerFee,
        BigDecimal agentCommission,
        BigDecimal bankShare
    ) {}

    StpDecision evaluateStp(StpEvaluationInput input);

    record StpEvaluationInput(
        String transactionType,
        UUID agentId,
        BigDecimal amount,
        String customerProfile,
        String agentTier,
        int transactionCountToday,
        BigDecimal amountToday,
        BigDecimal todayTotalAmount
    ) {}

    record StpDecision(String category, boolean approved, String reason) {}
}
