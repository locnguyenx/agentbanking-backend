package com.agentbanking.rules.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "velocity_rule")
public class VelocityRule {
    @Id
    private UUID ruleId;
    
    @Column(name = "rule_name")
    private String ruleName;
    
    @Column(name = "max_transactions_per_day")
    private int maxTransactionsPerDay;
    
    @Column(name = "max_amount_per_day")
    private BigDecimal maxAmountPerDay;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "scope")
    private VelocityScope scope;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;
    
    @Column(name = "is_active")
    private boolean active;

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public int getMaxTransactionsPerDay() { return maxTransactionsPerDay; }
    public void setMaxTransactionsPerDay(int maxTransactionsPerDay) { this.maxTransactionsPerDay = maxTransactionsPerDay; }
    public BigDecimal getMaxAmountPerDay() { return maxAmountPerDay; }
    public void setMaxAmountPerDay(BigDecimal maxAmountPerDay) { this.maxAmountPerDay = maxAmountPerDay; }
    public VelocityScope getScope() { return scope; }
    public void setScope(VelocityScope scope) { this.scope = scope; }
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
