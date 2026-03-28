package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.model.StpCategory;
import com.agentbanking.rules.domain.model.StpDecision;
import java.math.BigDecimal;

public class StpDecisionService {

    private final VelocityCheckService velocityCheckService;
    private final LimitEnforcementService limitEnforcementService;

    public StpDecisionService(VelocityCheckService velocityCheckService,
                               LimitEnforcementService limitEnforcementService) {
        this.velocityCheckService = velocityCheckService;
        this.limitEnforcementService = limitEnforcementService;
    }

    public StpDecision evaluate(String transactionType, String customerMykad,
                                 BigDecimal amount, AgentTier agentTier,
                                 int transactionCountToday, BigDecimal amountToday,
                                 FeeConfigRecord feeConfig, BigDecimal todayTotalAmount) {
        // Check velocity
        VelocityCheckService.VelocityCheckResult velocityResult = velocityCheckService
            .check(transactionCountToday, amountToday);

        // Check limits
        boolean limitPassed = limitEnforcementService
            .checkDailyLimit(amount, feeConfig, todayTotalAmount, transactionCountToday);

        // Determine STP category
        if (velocityResult.passed() && limitPassed) {
            return new StpDecision(
                StpCategory.FULL_STP,
                true,
                "Transaction approved - within velocity and limits",
                null,
                null
            );
        } else if (velocityResult.passed() || limitPassed) {
            return new StpDecision(
                StpCategory.CONDITIONAL_STP,
                false,
                "Requires manual review - partial pass",
                null,
                null
            );
        } else {
            return new StpDecision(
                StpCategory.NON_STP,
                false,
                "Requires manual approval - velocity/limit exceeded",
                null,
                null
            );
        }
    }

    public boolean isMicroAgentAutoApproval(String agentTier, BigDecimal amount) {
        return "MICRO".equals(agentTier) && amount.compareTo(new BigDecimal("500.00")) <= 0;
    }
}
