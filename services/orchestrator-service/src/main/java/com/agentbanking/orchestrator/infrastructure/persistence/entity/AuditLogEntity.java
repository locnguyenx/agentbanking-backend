package com.agentbanking.orchestrator.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    
    @Id
    @Column(name = "audit_id")
    private UUID auditId;
    
    @Column(name = "service_name", nullable = false, length = 50)
    private String serviceName;
    
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;
    
    @Column(name = "entity_id")
    private UUID entityId;
    
    @Column(name = "action", nullable = false, length = 50)
    private String action;
    
    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome;
    
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    
    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes;
    
    @Column(name = "trace_id", length = 100)
    private String traceId;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    @Column(name = "device_info", length = 500)
    private String deviceInfo;
    
    @Column(name = "geographic_location", length = 200)
    private String geographicLocation;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
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
    
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
    
    public String getGeographicLocation() { return geographicLocation; }
    public void setGeographicLocation(String geographicLocation) { this.geographicLocation = geographicLocation; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}