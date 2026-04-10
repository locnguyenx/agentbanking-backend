package com.agentbanking.orchestrator.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_resolution_case")
public class TransactionResolutionCaseEntity {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "proposed_action")
    private String proposedAction;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "evidence_url")
    private String evidenceUrl;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "maker_user_id")
    private String makerUserId;

    @Column(name = "maker_created_at")
    private Instant makerCreatedAt;

    @Column(name = "checker_user_id")
    private String checkerUserId;

    @Column(name = "checker_action")
    private String checkerAction;

    @Column(name = "checker_reason", columnDefinition = "TEXT")
    private String checkerReason;

    @Column(name = "checker_completed_at")
    private Instant checkerCompletedAt;

    @Column(name = "maker_pending_reason", length = 100)
    private String makerPendingReason;

    @Column(name = "checker_pending_reason", length = 100)
    private String checkerPendingReason;

    @Column(name = "temporal_signal_sent", nullable = false)
    private boolean temporalSignalSent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getWorkflowId() { return workflowId; }
    public void setWorkflowId(UUID workflowId) { this.workflowId = workflowId; }
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public String getProposedAction() { return proposedAction; }
    public void setProposedAction(String proposedAction) { this.proposedAction = proposedAction; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMakerUserId() { return makerUserId; }
    public void setMakerUserId(String makerUserId) { this.makerUserId = makerUserId; }
    public Instant getMakerCreatedAt() { return makerCreatedAt; }
    public void setMakerCreatedAt(Instant makerCreatedAt) { this.makerCreatedAt = makerCreatedAt; }
    public String getCheckerUserId() { return checkerUserId; }
    public void setCheckerUserId(String checkerUserId) { this.checkerUserId = checkerUserId; }
    public String getCheckerAction() { return checkerAction; }
    public void setCheckerAction(String checkerAction) { this.checkerAction = checkerAction; }
    public String getCheckerReason() { return checkerReason; }
    public void setCheckerReason(String checkerReason) { this.checkerReason = checkerReason; }
    public Instant getCheckerCompletedAt() { return checkerCompletedAt; }
    public void setCheckerCompletedAt(Instant checkerCompletedAt) { this.checkerCompletedAt = checkerCompletedAt; }
    public String getMakerPendingReason() { return makerPendingReason; }
    public void setMakerPendingReason(String makerPendingReason) { this.makerPendingReason = makerPendingReason; }
    public String getCheckerPendingReason() { return checkerPendingReason; }
    public void setCheckerPendingReason(String checkerPendingReason) { this.checkerPendingReason = checkerPendingReason; }
    public boolean isTemporalSignalSent() { return temporalSignalSent; }
    public void setTemporalSignalSent(boolean temporalSignalSent) { this.temporalSignalSent = temporalSignalSent; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
