package com.agentbanking.switchadapter.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "switch_transaction")
public class SwitchTransaction {
    @Id
    private UUID switchTxId;
    
    @Column(name = "internal_transaction_id")
    private UUID internalTransactionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "mt_type")
    private MessageType mtType;
    
    @Column(name = "iso_response_code")
    private String isoResponseCode;
    
    @Column(name = "switch_reference")
    private String switchReference;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SwitchStatus status;
    
    @Column(name = "original_reference")
    private String originalReference;
    
    @Column(name = "reversal_count")
    private int reversalCount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public UUID getSwitchTxId() { return switchTxId; }
    public void setSwitchTxId(UUID switchTxId) { this.switchTxId = switchTxId; }
    public UUID getInternalTransactionId() { return internalTransactionId; }
    public void setInternalTransactionId(UUID internalTransactionId) { this.internalTransactionId = internalTransactionId; }
    public MessageType getMtType() { return mtType; }
    public void setMtType(MessageType mtType) { this.mtType = mtType; }
    public String getIsoResponseCode() { return isoResponseCode; }
    public void setIsoResponseCode(String isoResponseCode) { this.isoResponseCode = isoResponseCode; }
    public String getSwitchReference() { return switchReference; }
    public void setSwitchReference(String switchReference) { this.switchReference = switchReference; }
    public SwitchStatus getStatus() { return status; }
    public void setStatus(SwitchStatus status) { this.status = status; }
    public String getOriginalReference() { return originalReference; }
    public void setOriginalReference(String originalReference) { this.originalReference = originalReference; }
    public int getReversalCount() { return reversalCount; }
    public void setReversalCount(int reversalCount) { this.reversalCount = reversalCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
