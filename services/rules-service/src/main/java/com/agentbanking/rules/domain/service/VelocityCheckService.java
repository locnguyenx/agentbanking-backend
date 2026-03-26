package com.agentbanking.rules.domain.service;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.rules.domain.model.VelocityRuleRecord;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import java.math.BigDecimal;
import java.util.List;

public class VelocityCheckService {

    private final VelocityRuleRepository velocityRuleRepository;

    public VelocityCheckService(VelocityRuleRepository velocityRuleRepository) {
        this.velocityRuleRepository = velocityRuleRepository;
    }

    public VelocityCheckResult check(int transactionCountToday, BigDecimal amountToday) {
        List<VelocityRuleRecord> rules = velocityRuleRepository.findActiveRules();
        for (VelocityRuleRecord rule : rules) {
            if (!rule.active()) {
                continue;
            }
            if (transactionCountToday >= rule.maxTransactionsPerDay()) {
                return new VelocityCheckResult(false, ErrorCodes.ERR_VELOCITY_COUNT_EXCEEDED);
            }
            if (amountToday.compareTo(rule.maxAmountPerDay()) > 0) {
                return new VelocityCheckResult(false, ErrorCodes.ERR_VELOCITY_AMOUNT_EXCEEDED);
            }
        }
        return new VelocityCheckResult(true, null);
    }

    public record VelocityCheckResult(boolean passed, String errorCode) {}
}
