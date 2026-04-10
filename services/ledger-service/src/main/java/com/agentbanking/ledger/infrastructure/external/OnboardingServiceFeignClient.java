package com.agentbanking.ledger.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * Feign client for querying agent data from Onboarding Service
 */
public interface OnboardingServiceFeignClient {


    @GetMapping("/internal/agents/{agentId}/pending-transactions")
    boolean hasPendingTransactions(@RequestParam("agentId") String agentId);
}
