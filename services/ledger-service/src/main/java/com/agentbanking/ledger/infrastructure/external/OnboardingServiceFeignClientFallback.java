package com.agentbanking.ledger.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;


@Component
public class OnboardingServiceFeignClientFallback implements OnboardingServiceFeignClient {


    @Override
    public boolean hasPendingTransactions(String agentId) {
        return false;
    }
}
