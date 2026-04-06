package com.agentbanking.orchestrator.application.activity;

import java.math.BigDecimal;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface CalculateMDRActivity {
    MDRResult calculate(String transactionType, String paymentMethod, BigDecimal amount);
    
    record MDRResult(BigDecimal mdrRate, BigDecimal mdrAmount, BigDecimal netAmount, String errorCode) {}
}
