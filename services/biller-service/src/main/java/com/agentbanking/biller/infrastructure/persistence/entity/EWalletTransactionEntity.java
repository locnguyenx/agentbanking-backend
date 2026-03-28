package com.agentbanking.biller.infrastructure.persistence.entity;

import com.agentbanking.biller.domain.model.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for e-Wallet transactions (Sarawak Pay withdrawal/top-up)
 */
@Entity
@Table(name = "ewallet_transaction")
public class EWalletTransactionEntity {
    @Id
    private UUID transactionId;
    
    @Column(name = "internal_transaction_id")
    private UUID internalTransactionId;
    
    @Column(name = "wallet_provider")
    private String walletProvider;
    
    @Column(name = "wallet_id")
    private String walletId;
    
    @Column(name = "amount")
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;
    
    @Column(name = "wallet_reference")
    private String walletReference;
    
    @Column(name = "agent_reference")
    private String agentReference;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public UUID getInternalTransactionId() { return internalTransactionId; }
    public void setInternalTransactionId(UUID internalTransactionId) { this.internalTransactionId = internalTransactionId; }
    public String getWalletProvider() { return walletProvider; }
    public void setWalletProvider(String walletProvider) { this.walletProvider = walletProvider; }
    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getWalletReference() { return walletReference; }
    public void setWalletReference(String walletReference) { this.walletReference = walletReference; }
    public String getAgentReference() { return agentReference; }
    public void setAgentReference(String agentReference) { this.agentReference = agentReference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}