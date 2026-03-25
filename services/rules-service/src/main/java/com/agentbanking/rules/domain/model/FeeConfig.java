package com.agentbanking.rules.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fee_config")
public class FeeConfig {
    @Id
    private UUID feeConfigId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_tier")
    private AgentTier agentTier;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type")
    private FeeType feeType;
    
    @Column(name = "customer_fee_value")
    private BigDecimal customerFeeValue;
    
    @Column(name = "agent_commission_value")
    private BigDecimal agentCommissionValue;
    
    @Column(name = "bank_share_value")
    private BigDecimal bankShareValue;
    
    @Column(name = "daily_limit_amount")
    private BigDecimal dailyLimitAmount;
    
    @Column(name = "daily_limit_count")
    private Integer dailyLimitCount;
    
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;
    
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    
    public UUID getFeeConfigId() { return feeConfigId; }
    public void setFeeConfigId(UUID feeConfigId) { this.feeConfigId = feeConfigId; }
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    public AgentTier getAgentTier() { return agentTier; }
    public void setAgentTier(AgentTier agentTier) { this.agentTier = agentTier; }
    public FeeType getFeeType() { return feeType; }
    public void setFeeType(FeeType feeType) { this.feeType = feeType; }
    public BigDecimal getCustomerFeeValue() { return customerFeeValue; }
    public void setCustomerFeeValue(BigDecimal customerFeeValue) { this.customerFeeValue = customerFeeValue; }
    public BigDecimal getAgentCommissionValue() { return agentCommissionValue; }
    public void setAgentCommissionValue(BigDecimal agentCommissionValue) { this.agentCommissionValue = agentCommissionValue; }
    public BigDecimal getBankShareValue() { return bankShareValue; }
    public void setBankShareValue(BigDecimal bankShareValue) { this.bankShareValue = bankShareValue; }
    public BigDecimal getDailyLimitAmount() { return dailyLimitAmount; }
    public void setDailyLimitAmount(BigDecimal dailyLimitAmount) { this.dailyLimitAmount = dailyLimitAmount; }
    public Integer getDailyLimitCount() { return dailyLimitCount; }
    public void setDailyLimitCount(Integer dailyLimitCount) { this.dailyLimitCount = dailyLimitCount; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
}
