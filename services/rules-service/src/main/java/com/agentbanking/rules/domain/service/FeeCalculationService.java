package com.agentbanking.rules.domain.service;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeCalculationService {

    private final FeeConfigRepository feeConfigRepository;

    public FeeCalculationService(FeeConfigRepository feeConfigRepository) {
        this.feeConfigRepository = feeConfigRepository;
    }

    @Transactional(readOnly = true)
    public FeeCalculationResult calculate(BigDecimal amount, TransactionType transactionType, AgentTier agentTier) {
        FeeConfigRecord config = feeConfigRepository.findByTransactionTypeAndAgentTier(
            transactionType, agentTier, LocalDate.now()
        ).orElseThrow(() -> new IllegalArgumentException(ErrorCodes.ERR_FEE_CONFIG_NOT_FOUND));

        BigDecimal customerFee = calculateComponent(amount, config.customerFeeValue(), config.feeType());
        BigDecimal agentCommission = calculateComponent(amount, config.agentCommissionValue(), config.feeType());
        BigDecimal bankShare = calculateComponent(amount, config.bankShareValue(), config.feeType());
        return new FeeCalculationResult(customerFee, agentCommission, bankShare);
    }

    private BigDecimal calculateComponent(BigDecimal amount, BigDecimal value, FeeType feeType) {
        if (feeType == FeeType.FIXED) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(value).setScale(2, RoundingMode.HALF_UP);
    }

    public record FeeCalculationResult(BigDecimal customerFee, BigDecimal agentCommission, BigDecimal bankShare) {}
}
