package com.agentbanking.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "agent_float")
public class AgentFloatEntity {
    @Id
    @Column(name = "float_id")
    private UUID floatId;
    
    @Column(name = "agent_id")
    private UUID agentId;
    
    @Column(name = "balance")
    private BigDecimal balance;
    
    @Column(name = "reserved_balance")
    private BigDecimal reservedBalance;
    
    @Column(name = "currency")
    private String currency;
    
    @Column(name = "merchant_gps_lat")
    private BigDecimal merchantGpsLat;
    
    @Column(name = "merchant_gps_lng")
    private BigDecimal merchantGpsLng;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    public UUID getFloatId() { return floatId; }
    public void setFloatId(UUID floatId) { this.floatId = floatId; }
    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = agentId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getReservedBalance() { return reservedBalance; }
    public void setReservedBalance(BigDecimal reservedBalance) { this.reservedBalance = reservedBalance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getMerchantGpsLat() { return merchantGpsLat; }
    public void setMerchantGpsLat(BigDecimal merchantGpsLat) { this.merchantGpsLat = merchantGpsLat; }
    public BigDecimal getMerchantGpsLng() { return merchantGpsLng; }
    public void setMerchantGpsLng(BigDecimal merchantGpsLng) { this.merchantGpsLng = merchantGpsLng; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
