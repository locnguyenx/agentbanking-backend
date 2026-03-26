package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.FeeConfig;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class LimitEnforcementService {

    public boolean checkDailyLimit(BigDecimal amount, FeeConfig config, BigDecimal todayTotalAmount, int todayTransactionCount) {
        // Check amount limit
        if (config.getDailyLimitAmount() != null) {
            BigDecimal projected = todayTotalAmount.add(amount);
            if (projected.compareTo(config.getDailyLimitAmount()) > 0) {
                return false;
            }
        }
        // Check count limit
        if (config.getDailyLimitCount() != null) {
            if (todayTransactionCount >= config.getDailyLimitCount()) {
                return false;
            }
        }
        return true;
    }
}
