package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;
import java.math.BigDecimal;
import com.agentbanking.orchestrator.domain.model.MDRResult;



@ActivityInterface
public interface CalculateMDRActivity {
    MDRResult calculate(String transactionType, String paymentMethod, BigDecimal amount);
}
