package com.agentbanking.onboarding.infrastructure.persistence.repository;

import java.util.UUID;

public interface TransactionQueryClient {
    boolean hasPendingTransactions(UUID agentId);
}
