package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.FeeConfigRecord;
import java.math.BigDecimal;

public class LimitEnforcementService {

    public boolean checkDailyLimit(BigDecimal amount, FeeConfigRecord config, BigDecimal todayTotalAmount, int todayTransactionCount) {
        if (config == null) {
            return true;
        }
        if (config.dailyLimitAmount() != null) {
            BigDecimal projected = todayTotalAmount.add(amount);
            if (projected.compareTo(config.dailyLimitAmount()) > 0) {
                return false;
            }
        }
        if (config.dailyLimitCount() != null) {
            if (todayTransactionCount >= config.dailyLimitCount()) {
                return false;
            }
        }
        return true;
    }
}
