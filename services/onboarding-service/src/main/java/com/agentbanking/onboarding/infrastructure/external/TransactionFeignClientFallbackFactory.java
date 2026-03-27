package com.agentbanking.onboarding.infrastructure.external;

import com.agentbanking.common.transaction.TransactionStatus;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class TransactionFeignClientFallbackFactory implements FallbackFactory<TransactionFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(TransactionFeignClientFallbackFactory.class);

    @Override
    public TransactionFeignClient create(Throwable cause) {
        return new TransactionFeignClient() {
            @Override
            public boolean hasPendingTransactions(UUID agentId) {
                log.warn("Circuit breaker fallback: hasPendingTransactions for agent {} failed: {}",
                        agentId, cause.getMessage());
                return false;
            }

            @Override
            public long countByAgentIdAndStatus(UUID agentId, TransactionStatus status) {
                log.warn("Circuit breaker fallback: countByAgentIdAndStatus for agent {} failed: {}",
                        agentId, cause.getMessage());
                return 0L;
            }

            @Override
            public boolean existsByAgentIdAndStatusIn(UUID agentId, List<TransactionStatus> statuses) {
                log.warn("Circuit breaker fallback: existsByAgentIdAndStatusIn for agent {} failed: {}",
                        agentId, cause.getMessage());
                return false;
            }
        };
    }
}