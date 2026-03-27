package com.agentbanking.ledger.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "rules-service", url = "${rules-service.url}", fallbackFactory = RulesServiceFeignClientFallback.class)
public interface RulesServiceFeignClient {

    @PostMapping("/internal/check-velocity")
    Map<String, Object> checkVelocity(@RequestBody Map<String, Object> request);
}
