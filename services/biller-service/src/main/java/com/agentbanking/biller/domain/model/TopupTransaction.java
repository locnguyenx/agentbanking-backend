package com.agentbanking.biller.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TopupTransaction {
    private UUID topupId;
    private UUID internalTransactionId;
    private String telco;
    private String phoneNumber;
    private BigDecimal amount;
    private PaymentStatus status;
    private String telcoReference;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public UUID getTopupId() { return topupId; }
    public void setTopupId(UUID topupId) { this.topupId = topupId; }
    public UUID getInternalTransactionId() { return internalTransactionId; }
    public void setInternalTransactionId(UUID internalTransactionId) { this.internalTransactionId = internalTransactionId; }
    public String getTelco() { return telco; }
    public void setTelco(String telco) { this.telco = telco; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getTelcoReference() { return telcoReference; }
    public void setTelcoReference(String telcoReference) { this.telcoReference = telcoReference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}