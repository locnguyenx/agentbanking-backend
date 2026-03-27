package com.agentbanking.common.audit;

public enum AuditAction {
    AGENT_CREATED,
    AGENT_UPDATED,
    AGENT_DEACTIVATED,
    WITHDRAWAL,
    DEPOSIT,
    BILL_PAYMENT,
    TRANSACTION_COMMITTED,
    TRANSACTION_ROLLED_BACK
}