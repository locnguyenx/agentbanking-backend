package com.agentbanking.rules.domain.port.in;

import com.agentbanking.rules.domain.model.FeeConfigRecord;
import java.math.BigDecimal;

public interface LimitEnforcementUseCase {

    boolean checkDailyLimit(BigDecimal amount, FeeConfigRecord config, BigDecimal todayTotalAmount, int todayTransactionCount);
}
