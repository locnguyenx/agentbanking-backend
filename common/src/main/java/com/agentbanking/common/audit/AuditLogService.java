package com.agentbanking.common.audit;

public interface AuditLogService {
    AuditLogRecord log(AuditLogRecord record);
}