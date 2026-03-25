package com.agentbanking.rules.domain.port.out;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.model.TransactionType;
import java.time.LocalDate;
import java.util.Optional;

public interface FeeConfigRepository {
    Optional<FeeConfig> findByTransactionTypeAndAgentTier(
        TransactionType transactionType, 
        AgentTier agentTier,
        LocalDate asOfDate);
}
