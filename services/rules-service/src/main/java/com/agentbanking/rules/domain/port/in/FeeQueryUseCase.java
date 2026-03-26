package com.agentbanking.rules.domain.port.in;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.TransactionType;
import java.math.BigDecimal;

public interface FeeQueryUseCase {

    FeeQueryResult calculate(BigDecimal amount, TransactionType transactionType, AgentTier agentTier);

    record FeeQueryResult(BigDecimal customerFee, BigDecimal agentCommission, BigDecimal bankShare) {}
}
