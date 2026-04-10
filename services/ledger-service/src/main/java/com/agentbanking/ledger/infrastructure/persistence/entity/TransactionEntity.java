package com.agentbanking.ledger.infrastructure.persistence.entity;

import com.agentbanking.ledger.domain.model.TransactionStatus;
import com.agentbanking.ledger.domain.model.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_transaction")
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id")
    private UUID transactionId;
    
    @Column(name = "idempotency_key")
    private String idempotencyKey;
    
    @Column(name = "agent_id")
    private UUID agentId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;
    
    @Column(name = "amount")
    private BigDecimal amount;
    
    @Column(name = "customer_fee")
    private BigDecimal customerFee;
    
    @Column(name = "agent_commission")
    private BigDecimal agentCommission;
    
    @Column(name = "bank_share")
    private BigDecimal bankShare;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status;
    
    @Column(name = "error_code")
    private String errorCode;
    
    @Column(name = "customer_mykad")
    private String customerMykad;
    
    @Column(name = "customer_card_masked")
    private String customerCardMasked;
    
    @Column(name = "switch_reference")
    private String switchReference;
    
    @Column(name = "reference_number")
    private String referenceNumber;
    
    @Column(name = "geofence_lat")
    private BigDecimal geofenceLat;
    
    @Column(name = "geofence_lng")
    private BigDecimal geofenceLng;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "agent_tier")
    private String agentTier;

    @Column(name = "target_bin")
    private String targetBin;

    @Column(name = "biller_code")
    private String billerCode;

    @Column(name = "ref1")
    private String ref1;

    @Column(name = "ref2")
    private String ref2;

    @Column(name = "destination_account")
    private String destinationAccount;

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = agentId; }
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getCustomerFee() { return customerFee; }
    public void setCustomerFee(BigDecimal customerFee) { this.customerFee = customerFee; }
    public BigDecimal getAgentCommission() { return agentCommission; }
    public void setAgentCommission(BigDecimal agentCommission) { this.agentCommission = agentCommission; }
    public BigDecimal getBankShare() { return bankShare; }
    public void setBankShare(BigDecimal bankShare) { this.bankShare = bankShare; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getCustomerMykad() { return customerMykad; }
    public void setCustomerMykad(String customerMykad) { this.customerMykad = customerMykad; }
    public String getCustomerCardMasked() { return customerCardMasked; }
    public void setCustomerCardMasked(String customerCardMasked) { this.customerCardMasked = customerCardMasked; }
    public String getSwitchReference() { return switchReference; }
    public void setSwitchReference(String switchReference) { this.switchReference = switchReference; }
    
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public BigDecimal getGeofenceLat() { return geofenceLat; }
    public void setGeofenceLat(BigDecimal geofenceLat) { this.geofenceLat = geofenceLat; }
    public BigDecimal getGeofenceLng() { return geofenceLng; }
    public void setGeofenceLng(BigDecimal geofenceLng) { this.geofenceLng = geofenceLng; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getAgentTier() { return agentTier; }
    public void setAgentTier(String agentTier) { this.agentTier = agentTier; }
    public String getTargetBin() { return targetBin; }
    public void setTargetBin(String targetBin) { this.targetBin = targetBin; }
    public String getBillerCode() { return billerCode; }
    public void setBillerCode(String billerCode) { this.billerCode = billerCode; }
    public String getRef1() { return ref1; }
    public void setRef1(String ref1) { this.ref1 = ref1; }
    public String getRef2() { return ref2; }
    public void setRef2(String ref2) { this.ref2 = ref2; }
    public String getDestinationAccount() { return destinationAccount; }
    public void setDestinationAccount(String destinationAccount) { this.destinationAccount = destinationAccount; }
}
