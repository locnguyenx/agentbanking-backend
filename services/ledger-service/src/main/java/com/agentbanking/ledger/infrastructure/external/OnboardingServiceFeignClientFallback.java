package com.agentbanking.ledger.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
public class OnboardingServiceFeignClientFallback implements OnboardingServiceFeignClient {


    @Override
    public boolean hasPendingTransactions(String agentId) {
        return false;
    }

    @Override
    public Object getAgentStats() {
        return Map.of("totalAgents", 0L, "activeAgents", 0L);
    }
}
