package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.AgentTier;
import com.agentbanking.ledger.domain.model.MdrCalculation;
import java.math.BigDecimal;

/**
 * Service for merchant-specific calculations (MDR, cash-back commission)
 */
public class MerchantTransactionService {

    public MdrCalculation calculateMdr(BigDecimal saleAmount, AgentTier merchantTier) {
        BigDecimal mdrRate = switch (merchantTier) {
            case MICRO -> new BigDecimal("0.015"); // 1.5%
            case STANDARD -> new BigDecimal("0.012"); // 1.2%
            case PREMIER -> new BigDecimal("0.010"); // 1.0%
            default -> BigDecimal.ZERO;
        };
        BigDecimal mdrAmount = saleAmount.multiply(mdrRate)
            .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal netToMerchant = saleAmount.subtract(mdrAmount);
        return new MdrCalculation(saleAmount, mdrRate, mdrAmount, netToMerchant);
    }

    public BigDecimal calculateCashBackCommission(BigDecimal cashBackAmount) {
        // Commission rate 0.5% of cash back amount
        return cashBackAmount.multiply(new BigDecimal("0.005"))
            .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate commission for PIN voucher purchase
     * Commission varies by agent tier:
     * - MICRO: 2.0%
     * - STANDARD: 1.5%
     * - PREMIER: 1.0%
     */
    public BigDecimal calculatePinPurchaseCommission(BigDecimal purchaseAmount, AgentTier agentTier) {
        BigDecimal commissionRate = switch (agentTier) {
            case MICRO -> new BigDecimal("0.020"); // 2.0%
            case STANDARD -> new BigDecimal("0.015"); // 1.5%
            case PREMIER -> new BigDecimal("0.010"); // 1.0%
            default -> BigDecimal.ZERO;
        };
        return purchaseAmount.multiply(commissionRate)
            .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
