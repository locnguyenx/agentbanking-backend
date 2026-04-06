package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.RulesServicePort;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckResult;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationResult;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.StpDecision;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "rules-service", url = "${rules-service.url}")
public interface RulesServiceClient {

    @PostMapping("/internal/check-velocity")
    VelocityCheckResult checkVelocity(@RequestBody VelocityCheckInput input);

    @PostMapping("/internal/calculate-fees")
    FeeCalculationResult calculateFees(@RequestBody FeeCalculationInput input);

    @PostMapping("/internal/evaluate-stp")
    StpDecision evaluateStp(@RequestParam("transactionType") String transactionType,
                            @RequestParam("agentId") UUID agentId,
                            @RequestParam("amount") BigDecimal amount,
                            @RequestParam("customerProfile") String customerProfile);
}
