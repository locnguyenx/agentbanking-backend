package com.agentbanking.rules.application.usecase;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.port.in.FeeQueryUseCase;
import com.agentbanking.rules.domain.service.FeeCalculationService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeQueryUseCaseImpl implements FeeQueryUseCase {

    private final FeeCalculationService feeCalculationService;

    public FeeQueryUseCaseImpl(FeeCalculationService feeCalculationService) {
        this.feeCalculationService = feeCalculationService;
    }

    @Override
    @Transactional(readOnly = true)
    public FeeQueryResult calculate(BigDecimal amount, TransactionType transactionType, AgentTier agentTier) {
        FeeCalculationService.FeeCalculationResult result =
            feeCalculationService.calculate(amount, transactionType, agentTier);
        return new FeeQueryResult(result.customerFee(), result.agentCommission(), result.bankShare());
    }
}
