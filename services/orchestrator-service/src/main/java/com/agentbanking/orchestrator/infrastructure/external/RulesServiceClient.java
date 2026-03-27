package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "rules-service", url = "${rules-service.url}")
public interface RulesServiceClient {

    @PostMapping("/internal/check-velocity")
    Map<String, Object> checkVelocity(@RequestBody Map<String, Object> request);

    @PostMapping("/internal/calculate-fees")
    Map<String, Object> calculateFees(@RequestBody Map<String, Object> request);
}
