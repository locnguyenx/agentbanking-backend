package com.agentbanking.orchestrator.infrastructure.adapter;

import com.agentbanking.orchestrator.domain.port.out.StoreAndForwardPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Store & Forward adapter for handling failed reversal messages.
 * Implements retry logic with different intervals for financial vs non-financial operations.
 */
@Component
public class StoreAndForwardAdapter implements StoreAndForwardPort {

    private static final Logger log = LoggerFactory.getLogger(StoreAndForwardAdapter.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // In-memory store for demonstration - in production, this would be a database
    private final ConcurrentHashMap<String, ReversalMessage> queue = new ConcurrentHashMap<>();

    // Retry intervals
    private static final long FINANCIAL_RETRY_INTERVAL_SECONDS = 60; // 1 minute for financial
    private static final long NON_FINANCIAL_RETRY_INTERVAL_SECONDS = 1; // Exponential for non-financial

    @Override
    public void queueReversalForRetry(ReversalMessage message) {
        log.info("Queuing reversal message for retry: transactionId={}, messageId={}",
                message.transactionId(), message.messageId());

        queue.put(message.messageId().toString(), message);

        // Schedule retry based on message type
        if (isFinancialReversal(message)) {
            // Financial reversals use fixed interval retry
            scheduleFinancialRetry(message);
        } else {
            // Non-financial operations use exponential backoff
            scheduleNonFinancialRetry(message);
        }
    }

    @Override
    public void processQueuedMessages() {
        log.debug("Processing queued Store & Forward messages: {} messages", queue.size());

        queue.forEach((messageId, message) -> {
            // Check if message has exceeded max retry attempts
            if (message.retryCount() >= getMaxRetries(message)) {
                log.warn("Message {} exceeded max retries ({}), flagging for manual intervention",
                        messageId, message.retryCount());
                handleMaxRetriesExceeded(message);
                queue.remove(messageId);
                return;
            }

            // Check if message is older than 24 hours
            if (isOlderThan24Hours(message)) {
                log.warn("Message {} older than 24 hours, flagging for manual intervention", messageId);
                handle24HourTimeout(message);
                queue.remove(messageId);
                return;
            }

            // Attempt to resend the message
            attemptResend(message);
        });
    }

    @Override
    public QueueStatistics getQueueStatistics() {
        int pendingMessages = queue.size();
        int totalRetries = queue.values().stream().mapToInt(ReversalMessage::retryCount).sum();
        long messagesOlderThan24Hours = queue.values().stream()
                .filter(this::isOlderThan24Hours)
                .count();

        return new QueueStatistics(pendingMessages, totalRetries, (int) messagesOlderThan24Hours);
    }

    private void scheduleFinancialRetry(ReversalMessage message) {
        scheduler.schedule(() -> {
            ReversalMessage updatedMessage = new ReversalMessage(
                message.messageId(),
                message.transactionId(),
                message.mti(),
                message.isoMessage(),
                message.retryCount() + 1,
                message.firstAttemptTime(),
                Instant.now(),
                null // Clear previous failure reason
            );
            queue.put(message.messageId().toString(), updatedMessage);
            attemptResend(updatedMessage);
        }, FINANCIAL_RETRY_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void scheduleNonFinancialRetry(ReversalMessage message) {
        // Exponential backoff: 1s, 2s, 4s, 8s, 16s, 32s, 64s...
        long delaySeconds = (long) Math.pow(2, message.retryCount());

        scheduler.schedule(() -> {
            ReversalMessage updatedMessage = new ReversalMessage(
                message.messageId(),
                message.transactionId(),
                message.mti(),
                message.isoMessage(),
                message.retryCount() + 1,
                message.firstAttemptTime(),
                Instant.now(),
                null
            );
            queue.put(message.messageId().toString(), updatedMessage);
            attemptResend(updatedMessage);
        }, Math.min(delaySeconds, 300), TimeUnit.SECONDS); // Cap at 5 minutes
    }

    private void attemptResend(ReversalMessage message) {
        log.info("Attempting to resend reversal message: attempt={}, transactionId={}",
                message.retryCount() + 1, message.transactionId());

        try {
            // In real implementation, this would send the ISO message to PayNet
            // For now, simulate occasional success/failure
            boolean success = simulateNetworkSend(message);

            if (success) {
                log.info("Reversal message sent successfully: transactionId={}", message.transactionId());
                queue.remove(message.messageId().toString());
                // TODO: Log successful reversal in audit
            } else {
                log.warn("Reversal message send failed: transactionId={}, attempt={}",
                        message.transactionId(), message.retryCount() + 1);

                // Update message with failure reason
                ReversalMessage failedMessage = new ReversalMessage(
                    message.messageId(),
                    message.transactionId(),
                    message.mti(),
                    message.isoMessage(),
                    message.retryCount(),
                    message.firstAttemptTime(),
                    message.lastAttemptTime(),
                    "Network timeout"
                );
                queue.put(message.messageId().toString(), failedMessage);

                // Schedule next retry
                if (isFinancialReversal(message)) {
                    scheduleFinancialRetry(failedMessage);
                } else {
                    scheduleNonFinancialRetry(failedMessage);
                }
            }

        } catch (Exception e) {
            log.error("Exception during reversal message resend: transactionId={}, error={}",
                    message.transactionId(), e.getMessage());
        }
    }

    private boolean simulateNetworkSend(ReversalMessage message) {
        // Simulate network conditions - occasional failures
        // In real implementation, this would actually send the ISO message
        return Math.random() > 0.3; // 70% success rate for simulation
    }

    private boolean isFinancialReversal(ReversalMessage message) {
        // Financial reversals use MTI 0400
        return "0400".equals(message.mti());
    }

    private int getMaxRetries(ReversalMessage message) {
        return isFinancialReversal(message) ? 1440 : 10; // 24 hours vs 10 attempts
    }

    private boolean isOlderThan24Hours(ReversalMessage message) {
        return message.firstAttemptTime().plusSeconds(24 * 60 * 60).isBefore(Instant.now());
    }

    private void handleMaxRetriesExceeded(ReversalMessage message) {
        log.error("Max retries exceeded for reversal message: transactionId={}, retries={}",
                message.transactionId(), message.retryCount());

        // TODO: Create alert for manual investigation
        // TODO: Update transaction status to REVERSAL_FAILED
        // TODO: Log in audit system
    }

    private void handle24HourTimeout(ReversalMessage message) {
        log.error("24-hour timeout exceeded for reversal message: transactionId={}",
                message.transactionId());

        // TODO: Create alert for manual investigation
        // TODO: Update transaction status to REVERSAL_STUCK
        // TODO: Log in audit system
    }
}