package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class FeeCalculationService {

    public static final FeeCalculationResult ZERO = new FeeCalculationResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    private final FeeConfigRepository feeConfigRepository;

    public FeeCalculationService(FeeConfigRepository feeConfigRepository) {
        this.feeConfigRepository = feeConfigRepository;
    }

    public FeeCalculationResult calculate(BigDecimal amount, TransactionType transactionType, AgentTier agentTier) {
        return feeConfigRepository.findByTransactionTypeAndAgentTier(
            transactionType, agentTier, LocalDate.now()
        ).map(config -> {
            BigDecimal customerFee = calculateComponent(amount, config.customerFeeValue(), config.feeType());
            BigDecimal agentCommission = calculateComponent(amount, config.agentCommissionValue(), config.feeType());
            BigDecimal bankShare = calculateComponent(amount, config.bankShareValue(), config.feeType());
            return new FeeCalculationResult(customerFee, agentCommission, bankShare);
        }).orElse(ZERO);
    }

    private BigDecimal calculateComponent(BigDecimal amount, BigDecimal value, FeeType feeType) {
        if (feeType == FeeType.FIXED) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(value).setScale(2, RoundingMode.HALF_UP);
    }

    public record FeeCalculationResult(BigDecimal customerFee, BigDecimal agentCommission, BigDecimal bankShare) {}
}
