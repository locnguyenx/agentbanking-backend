package com.agentbanking.ledger.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface GetBalanceUseCase {
    BigDecimal getBalance(UUID agentId);
}