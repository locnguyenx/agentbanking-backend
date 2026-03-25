package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.VelocityRule;
import java.math.BigDecimal;
import java.util.List;

public class VelocityCheckService {

    public VelocityCheckResult check(List<VelocityRule> rules, int transactionCountToday, BigDecimal amountToday) {
        for (VelocityRule rule : rules) {
            if (!rule.isActive()) {
                continue;
            }
            if (transactionCountToday >= rule.getMaxTransactionsPerDay()) {
                return new VelocityCheckResult(false, "ERR_VELOCITY_COUNT_EXCEEDED");
            }
            if (amountToday.compareTo(rule.getMaxAmountPerDay()) > 0) {
                return new VelocityCheckResult(false, "ERR_VELOCITY_AMOUNT_EXCEEDED");
            }
        }
        return new VelocityCheckResult(true, null);
    }

    public record VelocityCheckResult(boolean passed, String errorCode) {}
}
