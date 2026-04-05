package com.agentbanking.audit.infrastructure.persistence;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp DESC"),
    @Index(name = "idx_audit_logs_service", columnList = "service_name"),
    @Index(name = "idx_audit_logs_action", columnList = "action"),
    @Index(name = "idx_audit_logs_performed_by", columnList = "performed_by"),
    @Index(name = "idx_audit_logs_outcome", columnList = "outcome")
})
public class JpaAuditLogEntity {

    @Id private UUID auditId;
    @Column(name = "service_name", nullable = false, length = 50) private String serviceName;
    @Column(name = "entity_type", nullable = false, length = 50) private String entityType;
    @Column(name = "entity_id") private UUID entityId;
    @Column(nullable = false, length = 50) private String action;
    @Column(name = "performed_by", nullable = false, length = 100) private String performedBy;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(nullable = false) private LocalDateTime timestamp;
    @Column(nullable = false, length = 20) private String outcome;
    @Column(name = "failure_reason", length = 500) private String failureReason;
    @Column(columnDefinition = "TEXT") private String changes;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public JpaAuditLogEntity() {}

    public JpaAuditLogEntity(AuditLogRecord record) {
        this.auditId = record.auditId();
        this.serviceName = record.serviceName();
        this.entityType = record.entityType();
        this.entityId = record.entityId();
        this.action = record.action();
        this.performedBy = record.performedBy();
        this.ipAddress = record.ipAddress();
        this.timestamp = record.timestamp();
        this.outcome = record.outcome();
        this.failureReason = record.failureReason();
        this.changes = record.changes();
        this.createdAt = record.createdAt();
    }

    public AuditLogRecord toRecord() {
        return new AuditLogRecord(
            auditId, serviceName, entityType, entityId, action,
            performedBy, ipAddress, timestamp, outcome, failureReason, changes, createdAt
        );
    }

    public UUID getAuditId() { return auditId; }
    public void setAuditId(UUID auditId) { this.auditId = auditId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getChanges() { return changes; }
    public void setChanges(String changes) { this.changes = changes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
