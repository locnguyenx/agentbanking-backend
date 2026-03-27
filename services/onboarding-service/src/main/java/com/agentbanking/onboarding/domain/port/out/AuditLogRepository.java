package com.agentbanking.onboarding.domain.port.out;

import com.agentbanking.common.audit.AuditLogRecord;

public interface AuditLogRepository {
    AuditLogRecord save(AuditLogRecord record);
}
