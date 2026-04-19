package com.agentbanking.rules.domain.port.in;

import com.agentbanking.rules.domain.model.TransactionType;
import java.math.BigDecimal;

public interface VelocityCheckUseCase {

    VelocityCheckResult check(String agentId, TransactionType transactionType, int transactionCountToday, BigDecimal amountToday);

    record VelocityCheckResult(boolean passed, String errorCode) {}
}
