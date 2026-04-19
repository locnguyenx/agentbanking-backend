package com.agentbanking.rules.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.model.VelocityRuleRecord;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import java.math.BigDecimal;
import java.util.List;

public class VelocityCheckService {

    private static final Logger log = LoggerFactory.getLogger(VelocityCheckService.class);

    private final VelocityRuleRepository velocityRuleRepository;

    public VelocityCheckService(VelocityRuleRepository velocityRuleRepository) {
        this.velocityRuleRepository = velocityRuleRepository;
    }

    public VelocityCheckResult check(String agentId, TransactionType transactionType, int transactionCountToday, BigDecimal amountToday) {
        List<VelocityRuleRecord> rules = velocityRuleRepository.findActiveRules();
        log.info("Checking velocity for agent: {}, type: {}, count: {}, amount: {}. Total active rules: {}", 
                 agentId, transactionType, transactionCountToday, amountToday, rules.size());
        for (VelocityRuleRecord rule : rules) {
            log.debug("Evaluating rule: {} (Type: {}, MaxCount: {})", 
                      rule.ruleName(), rule.transactionType(), rule.maxTransactionsPerDay());
            if (!rule.active()) {
                continue;
            }
            
            // Filter by transaction type if specified in the rule
            if (rule.transactionType() != null && rule.transactionType() != transactionType) {
                log.debug("Skipping rule {} due to type mismatch: ruleType={}, requestType={}", 
                          rule.ruleName(), rule.transactionType(), transactionType);
                continue;
            }

            if (transactionCountToday >= rule.maxTransactionsPerDay()) {
                log.warn("Velocity count exceeded for agent {}: {} >= {}", 
                         agentId, transactionCountToday, rule.maxTransactionsPerDay());
                return new VelocityCheckResult(false, ErrorCodes.ERR_VELOCITY_COUNT_EXCEEDED_CORRECT);
            }
            if (amountToday.compareTo(rule.maxAmountPerDay()) > 0) {
                return new VelocityCheckResult(false, ErrorCodes.ERR_VELOCITY_AMOUNT_EXCEEDED_CORRECT);
            }
        }
        return new VelocityCheckResult(true, null);
    }

    public record VelocityCheckResult(boolean passed, String errorCode) {}
}
