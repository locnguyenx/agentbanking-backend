package com.agentbanking.biller.infrastructure.persistence.entity;

import com.agentbanking.biller.domain.model.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bill_payment")
public class BillPaymentEntity {
    @Id
    private UUID paymentId;
    
    @Column(name = "biller_id")
    private UUID billerId;
    
    @Column(name = "internal_transaction_id")
    private UUID internalTransactionId;
    
    @Column(name = "ref1")
    private String ref1;
    
    @Column(name = "ref2")
    private String ref2;
    
    @Column(name = "amount")
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;
    
    @Column(name = "receipt_no")
    private String receiptNo;
    
    @Column(name = "biller_reference")
    private String billerReference;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public UUID getBillerId() { return billerId; }
    public void setBillerId(UUID billerId) { this.billerId = billerId; }
    public UUID getInternalTransactionId() { return internalTransactionId; }
    public void setInternalTransactionId(UUID internalTransactionId) { this.internalTransactionId = internalTransactionId; }
    public String getRef1() { return ref1; }
    public void setRef1(String ref1) { this.ref1 = ref1; }
    public String getRef2() { return ref2; }
    public void setRef2(String ref2) { this.ref2 = ref2; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
    public String getBillerReference() { return billerReference; }
    public void setBillerReference(String billerReference) { this.billerReference = billerReference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}