package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.RulesServicePort;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckResult;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rules-service", url = "${rules-service.url}")
public interface RulesServiceClient {

    @PostMapping("/internal/check-velocity")
    VelocityCheckResult checkVelocity(@RequestBody VelocityCheckInput input);

    @PostMapping("/internal/calculate-fees")
    FeeCalculationResult calculateFees(@RequestBody FeeCalculationInput input);
}
