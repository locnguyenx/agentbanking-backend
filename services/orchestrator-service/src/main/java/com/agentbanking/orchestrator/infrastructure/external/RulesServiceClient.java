package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckResult;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationResult;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.StpDecision;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rules-service", url = "${rules-service.url}", fallbackFactory = RulesServiceClientFallbackFactory.class)
public interface RulesServiceClient {

    @PostMapping("/internal/check-velocity")
    VelocityCheckResult checkVelocity(@RequestBody VelocityCheckInput input);

    @PostMapping("/internal/calculate-fees")
    FeeCalculationResult calculateFees(@RequestBody FeeCalculationInput input);

    @PostMapping("/internal/stp/evaluate")
    StpDecision evaluateStp(@RequestBody StpEvaluationRequest request);

    record StpEvaluationRequest(
        String transactionType,
        String agentId,
        String amount,
        String customerProfile,
        String agentTier,
        int transactionCountToday,
        String amountToday,
        String todayTotalAmount
    ) {}
}
