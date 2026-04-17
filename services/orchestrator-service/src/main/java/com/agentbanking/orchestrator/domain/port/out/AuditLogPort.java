package com.agentbanking.orchestrator.domain.port.out;

import java.util.UUID;

/**
 * Port for audit logging operations in the Safety Reversal process.
 * Used by the SendReversalToSwitchActivity to log reversal attempts and outcomes.
 */
public interface AuditLogPort {

    /**
     * Log successful safety reversal with retry count
     */
    void logSafetyReversalSuccess(UUID transactionId, int retryCount);

    /**
     * Log failed safety reversal after max retries
     */
    void logSafetyReversalFailed(UUID transactionId, int retryCount);

    /**
     * Log safety reversal stuck after 24 hours - flagged for manual intervention
     */
    void logSafetyReversalStuck(UUID transactionId, int retryCount);
}