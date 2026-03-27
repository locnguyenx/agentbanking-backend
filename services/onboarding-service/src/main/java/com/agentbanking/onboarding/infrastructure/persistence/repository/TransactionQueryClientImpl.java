package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.common.transaction.TransactionStatus;
import com.agentbanking.onboarding.infrastructure.external.TransactionFeignClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TransactionQueryClientImpl implements TransactionQueryClient {

    private final TransactionFeignClient feignClient;

    public TransactionQueryClientImpl(TransactionFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    @Override
    public boolean hasPendingTransactions(UUID agentId) {
        List<TransactionStatus> pendingStatuses = List.of(TransactionStatus.PENDING, TransactionStatus.COMPLETED);
        return feignClient.existsByAgentIdAndStatusIn(agentId, pendingStatuses);
    }
}
