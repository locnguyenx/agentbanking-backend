package com.agentbanking.rules.application.usecase;

import com.agentbanking.rules.domain.port.in.VelocityCheckUseCase;
import com.agentbanking.rules.domain.service.VelocityCheckService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VelocityCheckUseCaseImpl implements VelocityCheckUseCase {

    private final VelocityCheckService velocityCheckService;

    public VelocityCheckUseCaseImpl(VelocityCheckService velocityCheckService) {
        this.velocityCheckService = velocityCheckService;
    }

    @Override
    @Transactional(readOnly = true)
    public VelocityCheckResult check(int transactionCountToday, BigDecimal amountToday) {
        VelocityCheckService.VelocityCheckResult result =
            velocityCheckService.check(transactionCountToday, amountToday);
        return new VelocityCheckResult(result.passed(), result.errorCode());
    }
}
