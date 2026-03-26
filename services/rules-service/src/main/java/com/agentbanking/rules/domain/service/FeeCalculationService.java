package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.model.FeeType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class FeeCalculationService {

    public FeeCalculationResult calculate(BigDecimal amount, FeeConfig config) {
        BigDecimal customerFee = calculateComponent(amount, config.getCustomerFeeValue(), config.getFeeType());
        BigDecimal agentCommission = calculateComponent(amount, config.getAgentCommissionValue(), config.getFeeType());
        BigDecimal bankShare = calculateComponent(amount, config.getBankShareValue(), config.getFeeType());
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
