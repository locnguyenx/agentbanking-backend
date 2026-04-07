package com.agentbanking.rules.application.usecase;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.rules.domain.port.in.TransactionQuoteUseCase;
import com.agentbanking.rules.domain.service.FeeCalculationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class TransactionQuoteUseCaseImpl implements TransactionQuoteUseCase {

    private final FeeCalculationService feeCalculationService;

    public TransactionQuoteUseCaseImpl(FeeCalculationService feeCalculationService) {
        this.feeCalculationService = feeCalculationService;
    }

    @Override
    public QuoteResult calculateQuote(String agentId, String agentTier, String amount,
                                      String serviceCode, String fundingSource, String billerRouting) {
        if (amount == null || amount.isBlank()) {
            throw new IllegalArgumentException("amount is required");
        }
        if (serviceCode == null || serviceCode.isBlank()) {
            throw new IllegalArgumentException("serviceCode is required");
        }
        if (agentTier == null || agentTier.isBlank()) {
            throw new IllegalArgumentException("agentTier is required");
        }

        try {
            BigDecimal amountDecimal = new BigDecimal(amount);

            FeeCalculationService.FeeCalculationResult feeResult = feeCalculationService.calculate(
                amountDecimal, 
                com.agentbanking.rules.domain.model.TransactionType.valueOf(serviceCode),
                com.agentbanking.rules.domain.model.AgentTier.valueOf(agentTier)
            );

            BigDecimal total = amountDecimal.add(feeResult.customerFee());

            return new QuoteResult(
                "QT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                amountDecimal.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                feeResult.customerFee().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                total.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                feeResult.agentCommission().setScale(2, RoundingMode.HALF_UP).toPlainString()
            );
        } catch (Exception e) {
            throw new IllegalStateException(ErrorCodes.ERR_BIZ_QUOTE_CALCULATION_FAILED + ": " + e.getMessage(), e);
        }
    }
}
