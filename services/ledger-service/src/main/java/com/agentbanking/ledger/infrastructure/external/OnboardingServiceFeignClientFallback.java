package com.agentbanking.ledger.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

import com.agentbanking.onboarding.domain.model.AgentRecord;

@Component
public class OnboardingServiceFeignClientFallback implements OnboardingServiceFeignClient {

    @Override
    public AgentRecord getAgent(String agentId) {
        return null;
    }

    @Override
    public boolean hasPendingTransactions(String agentId) {
        return false;
    }
}
