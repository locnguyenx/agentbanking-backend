package com.agentbanking.onboarding.infrastructure.external;

import com.agentbanking.common.transaction.TransactionStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(
    name = "ledger-service",
    url = "${services.ledger-service.url:http://localhost:8081}",
    fallbackFactory = TransactionFeignClientFallbackFactory.class
)
public interface TransactionFeignClient {

    @GetMapping("/internal/transactions/has-pending")
    boolean hasPendingTransactions(@RequestParam("agentId") UUID agentId);

    @GetMapping("/internal/transactions/count-by-status")
    long countByAgentIdAndStatus(
        @RequestParam("agentId") UUID agentId,
        @RequestParam("status") TransactionStatus status
    );

    @GetMapping("/internal/transactions/exists-by-status")
    boolean existsByAgentIdAndStatusIn(
        @RequestParam("agentId") UUID agentId,
        @RequestParam("statuses") List<TransactionStatus> statuses
    );
}
