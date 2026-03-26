package com.agentbanking.rules.domain.port.in;

import java.math.BigDecimal;

public interface VelocityCheckUseCase {

    VelocityCheckResult check(int transactionCountToday, BigDecimal amountToday);

    record VelocityCheckResult(boolean passed, String errorCode) {}
}
