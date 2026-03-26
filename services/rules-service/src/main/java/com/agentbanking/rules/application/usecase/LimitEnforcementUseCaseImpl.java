package com.agentbanking.rules.application.usecase;

import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.port.in.LimitEnforcementUseCase;
import com.agentbanking.rules.domain.service.LimitEnforcementService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class LimitEnforcementUseCaseImpl implements LimitEnforcementUseCase {

    private final LimitEnforcementService limitEnforcementService;

    public LimitEnforcementUseCaseImpl(LimitEnforcementService limitEnforcementService) {
        this.limitEnforcementService = limitEnforcementService;
    }

    @Override
    public boolean checkDailyLimit(BigDecimal amount, FeeConfigRecord config,
                                   BigDecimal todayTotalAmount, int todayTransactionCount) {
        return limitEnforcementService.checkDailyLimit(amount, config, todayTotalAmount, todayTransactionCount);
    }
}
