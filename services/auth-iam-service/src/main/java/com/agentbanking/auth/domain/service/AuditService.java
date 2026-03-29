package com.agentbanking.auth.domain.service;

import com.agentbanking.auth.domain.model.AuditLogRecord;
import com.agentbanking.auth.domain.port.out.AuditLogRepository;

import java.util.List;
import java.util.UUID;

/**
 * Domain service for audit logging
 */
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log an audit event
     * @param auditLogRecord the audit log record to persist
     * @return the persisted audit log record
     */
    public AuditLogRecord logAuditEvent(AuditLogRecord auditLogRecord) {
        return auditLogRepository.save(auditLogRecord);
    }

    /**
     * Get audit log by ID
     * @param auditId the audit log ID
     * @return the audit log or null if not found
     */
    public AuditLogRecord getAuditLogById(UUID auditId) {
        return auditLogRepository.findById(auditId).orElse(null);
    }

    /**
     * Get all audit logs (for admin purposes)
     * Note: In a production system, this should be paginated and filtered
     * @return list of audit logs
     */
    public List<AuditLogRecord> getAllAuditLogs() {
        // In a full implementation, we would have a method to get all audit logs
        // For now, return empty list as this requires a query method
        return List.of();
    }
}