package com.agentbanking.orchestrator.infrastructure.adapter;

import com.agentbanking.orchestrator.domain.port.out.AuditLogPort;
import com.agentbanking.orchestrator.domain.port.out.AuditLogRepository;
import com.agentbanking.common.audit.AuditLogRecord;
import com.agentbanking.common.audit.AuditAction;
import com.agentbanking.common.audit.AuditOutcome;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class AuditLogAdapter implements AuditLogPort {

    private final AuditLogRepository auditLogRepository;

    public AuditLogAdapter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void logSafetyReversalSuccess(UUID transactionId, int retryCount) {
        AuditLogRecord record = new AuditLogRecord(
            UUID.randomUUID(),
            "TRANSACTION",
            transactionId,
            AuditAction.SAFETY_REVERSAL_SUCCESS,
            "system",
            "Safety reversal succeeded after " + retryCount + " attempts",
            "system",
            LocalDateTime.now(),
            AuditOutcome.SUCCESS,
            null,
            null,
            null,
            "orchestrator-service",
            null,
            null
        );
        auditLogRepository.save(record);
    }

    @Override
    public void logSafetyReversalFailed(UUID transactionId, int retryCount) {
        AuditLogRecord record = new AuditLogRecord(
            UUID.randomUUID(),
            "TRANSACTION",
            transactionId,
            AuditAction.SAFETY_REVERSAL_FAILED,
            "system",
            "Safety reversal failed after " + retryCount + " attempts",
            "system",
            LocalDateTime.now(),
            AuditOutcome.FAILURE,
            "Max retries exceeded",
            null,
            null,
            "orchestrator-service",
            null,
            null
        );
        auditLogRepository.save(record);
    }

    @Override
    public void logSafetyReversalStuck(UUID transactionId, int retryCount) {
        AuditLogRecord record = new AuditLogRecord(
            UUID.randomUUID(),
            "TRANSACTION",
            transactionId,
            AuditAction.SAFETY_REVERSAL_STUCK,
            "system",
            "Safety reversal stuck after 24 hours, flagged for manual intervention",
            "system",
            LocalDateTime.now(),
            AuditOutcome.FAILURE,
            "24-hour timeout exceeded",
            null,
            null,
            "orchestrator-service",
            null,
            null
        );
        auditLogRepository.save(record);
    }
}