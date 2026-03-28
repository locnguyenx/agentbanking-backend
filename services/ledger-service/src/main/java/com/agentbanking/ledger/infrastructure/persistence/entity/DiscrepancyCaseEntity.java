package com.agentbanking.ledger.infrastructure.persistence.entity;

import com.agentbanking.ledger.domain.model.DiscrepancyStatus;
import com.agentbanking.ledger.domain.model.DiscrepancyType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "discrepancy_case")
public class DiscrepancyCaseEntity {

    @Id
    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type", nullable = false)
    private DiscrepancyType discrepancyType;

    @Column(name = "internal_amount")
    private BigDecimal internalAmount;

    @Column(name = "network_amount")
    private BigDecimal networkAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DiscrepancyStatus status;

    @Column(name = "maker_action")
    private String makerAction;

    @Column(name = "maker_user_id")
    private String makerUserId;

    @Column(name = "maker_reason")
    @Lob
    private String makerReason;

    @Column(name = "checker_user_id")
    private String checkerUserId;

    @Column(name = "checker_action")
    private String checkerAction;

    @Column(name = "checker_reason")
    @Lob
    private String checkerReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public DiscrepancyType getDiscrepancyType() { return discrepancyType; }
    public void setDiscrepancyType(DiscrepancyType discrepancyType) { this.discrepancyType = discrepancyType; }
    public BigDecimal getInternalAmount() { return internalAmount; }
    public void setInternalAmount(BigDecimal internalAmount) { this.internalAmount = internalAmount; }
    public BigDecimal getNetworkAmount() { return networkAmount; }
    public void setNetworkAmount(BigDecimal networkAmount) { this.networkAmount = networkAmount; }
    public DiscrepancyStatus getStatus() { return status; }
    public void setStatus(DiscrepancyStatus status) { this.status = status; }
    public String getMakerAction() { return makerAction; }
    public void setMakerAction(String makerAction) { this.makerAction = makerAction; }
    public String getMakerUserId() { return makerUserId; }
    public void setMakerUserId(String makerUserId) { this.makerUserId = makerUserId; }
    public String getMakerReason() { return makerReason; }
    public void setMakerReason(String makerReason) { this.makerReason = makerReason; }
    public String getCheckerUserId() { return checkerUserId; }
    public void setCheckerUserId(String checkerUserId) { this.checkerUserId = checkerUserId; }
    public String getCheckerAction() { return checkerAction; }
    public void setCheckerAction(String checkerAction) { this.checkerAction = checkerAction; }
    public String getCheckerReason() { return checkerReason; }
    public void setCheckerReason(String checkerReason) { this.checkerReason = checkerReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
