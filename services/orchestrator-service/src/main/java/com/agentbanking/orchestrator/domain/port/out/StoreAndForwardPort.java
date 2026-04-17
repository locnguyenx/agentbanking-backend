package com.agentbanking.orchestrator.domain.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Port for Store & Forward operations - persistent queuing for failed messages
 * that need to be retried across network outages and JVM restarts.
 */
public interface StoreAndForwardPort {

    /**
     * Queue a reversal for Store & Forward retry
     */
    void queueReversalForRetry(ReversalMessage message);

    /**
     * Process queued messages - called by background job
     */
    void processQueuedMessages();

    /**
     * Get queue statistics
     */
    QueueStatistics getQueueStatistics();

    record ReversalMessage(
        String messageId,
        UUID transactionId,
        String mti, // MTI 0400 for reversals
        String isoMessage,
        int retryCount,
        Instant firstAttemptTime,
        Instant lastAttemptTime,
        String failureReason
    ) {}

    record QueueStatistics(
        int pendingMessages,
        int totalRetries,
        int messagesOlderThan24Hours
    ) {}
}