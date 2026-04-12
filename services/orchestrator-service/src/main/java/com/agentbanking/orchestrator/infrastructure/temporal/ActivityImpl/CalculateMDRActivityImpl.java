package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.CalculateMDRActivity;
import com.agentbanking.orchestrator.domain.model.MDRResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CalculateMDRActivityImpl implements CalculateMDRActivity {

    @Override
    public MDRResult calculate(String transactionType, String paymentMethod, BigDecimal amount) {
        BigDecimal mdrRate = switch (paymentMethod.toUpperCase()) {
            case "QR" -> new BigDecimal("0.008"); // 0.8%
            case "RTP" -> new BigDecimal("0.005"); // 0.5%
            case "CARD" -> new BigDecimal("0.015"); // 1.5%
            default -> new BigDecimal("0.01"); // 1% default
        };
        
        BigDecimal mdrAmount = amount.multiply(mdrRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = amount.subtract(mdrAmount);
        
        return new MDRResult(mdrRate, mdrAmount, netAmount, null);
    }
}
