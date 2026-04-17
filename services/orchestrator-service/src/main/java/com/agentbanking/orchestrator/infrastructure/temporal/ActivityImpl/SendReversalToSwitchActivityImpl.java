package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.SendReversalToSwitchActivity;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalResult;
import com.agentbanking.orchestrator.domain.port.out.AuditLogPort;
import com.agentbanking.orchestrator.domain.port.out.StoreAndForwardPort;

import io.temporal.activity.Activity;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

import java.time.Duration;
import java.time.Instant;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class SendReversalToSwitchActivityImpl implements SendReversalToSwitchActivity {

    private static final Logger log = LoggerFactory.getLogger(SendReversalToSwitchActivityImpl.class);

    private final SwitchAdapterPort switchAdapterPort;
    private final AuditLogPort auditLogPort;
    private final StoreAndForwardPort storeAndForwardPort;

    // BDD-SR Constants
    private static final int MAX_RETRIES = 1440; // 24 hours at 60s intervals
    private static final Duration RETRY_INTERVAL = Duration.ofSeconds(60);

    public SendReversalToSwitchActivityImpl(SwitchAdapterPort switchAdapterPort, AuditLogPort auditLogPort, StoreAndForwardPort storeAndForwardPort) {
        this.switchAdapterPort = switchAdapterPort;
        this.auditLogPort = auditLogPort;
        this.storeAndForwardPort = storeAndForwardPort;
    }

    @Override
    public SwitchReversalResult sendReversal(SwitchReversalInput input) {
        return switchAdapterPort.sendReversal(input);
    }

    @Override
    public SafetyReversalResult sendReversalWithRetry(SwitchReversalInput input) {
        int retryCount = 0;
        Instant startTime = Instant.now();

        while (retryCount < MAX_RETRIES) {
            try {
                log.info("Safety Reversal attempt {} for transaction {}", retryCount + 1, input.internalTransactionId());

                SwitchReversalResult result = switchAdapterPort.sendReversal(input);

                if (result.success()) {
                    log.info("Safety Reversal succeeded on attempt {} for transaction {}",
                        retryCount + 1, input.internalTransactionId());

                    // Log successful reversal
                    auditLogPort.logSafetyReversalSuccess(input.internalTransactionId(), retryCount + 1);

                    return new SafetyReversalResult(true, null, retryCount + 1, false);
                } else {
                    log.warn("Safety Reversal attempt {} failed for transaction {}: {}",
                        retryCount + 1, input.internalTransactionId(), result.errorCode());

                    retryCount++;

                    // Check if we've exceeded 24 hours (BDD-SR-04)
                    Duration elapsed = Duration.between(startTime, Instant.now());
                    if (elapsed.toHours() >= 24) {
                        log.error("Safety Reversal failed for 24 hours, flagging for manual intervention: {}",
                            input.internalTransactionId());

                        auditLogPort.logSafetyReversalStuck(input.internalTransactionId(), retryCount);

                        return new SafetyReversalResult(false, "REVERSAL_TIMEOUT", retryCount, true);
                    }

                    // Wait before retry (BDD-SR-02)
                    if (retryCount < MAX_RETRIES) {
                        try {
                            Thread.sleep(RETRY_INTERVAL.toMillis());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                log.error("Safety Reversal attempt {} threw exception for transaction {}: {}",
                    retryCount + 1, input.internalTransactionId(), e.getMessage());

                retryCount++;

                // Check timeout
                Duration elapsed = Duration.between(startTime, Instant.now());
                if (elapsed.toHours() >= 24) {
                    auditLogPort.logSafetyReversalStuck(input.internalTransactionId(), retryCount);
                    return new SafetyReversalResult(false, "REVERSAL_EXCEPTION_TIMEOUT", retryCount, true);
                }

                // For network-related exceptions, use Store & Forward (BDD-V01-EC-01)
                if (isNetworkException(e)) {
                    log.info("Network exception detected, queuing for Store & Forward retry: transactionId={}",
                        input.internalTransactionId());

                    // Create Store & Forward message
                    StoreAndForwardPort.ReversalMessage sfMessage = new StoreAndForwardPort.ReversalMessage(
                        UUID.randomUUID().toString(), // messageId as String
                        input.internalTransactionId(), // transactionId as UUID
                        "0400", // MTI for reversal
                        createIsoMessage(input), // Would create actual ISO message
                        retryCount,
                        Instant.now(),
                        Instant.now(),
                        e.getMessage()
                    );

                    storeAndForwardPort.queueReversalForRetry(sfMessage);
                    return new SafetyReversalResult(false, "QUEUED_FOR_STORE_FORWARD", retryCount, false);
                }

                // For other exceptions, continue with direct retry
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_INTERVAL.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // Max retries exceeded
        log.error("Safety Reversal max retries exceeded for transaction {}", input.internalTransactionId());
        auditLogPort.logSafetyReversalFailed(input.internalTransactionId(), retryCount);

        return new SafetyReversalResult(false, "MAX_RETRIES_EXCEEDED", retryCount, true);
    }

    /**
     * Determines if an exception is network-related for Store & Forward queuing.
     */
    private boolean isNetworkException(Exception e) {
        // In a real implementation, this would check for specific network-related exceptions
        // For now, treat timeouts and connection issues as network exceptions
        String message = e.getMessage();
        return message != null && (
            message.contains("timeout") ||
            message.contains("connection") ||
            message.contains("network") ||
            message.contains("unreachable")
        );
    }

    /**
     * Creates an ISO 8583 message for the reversal.
     * In a real implementation, this would format the actual ISO message.
     */
    private String createIsoMessage(SwitchReversalInput input) {
        // Placeholder for ISO 8583 message creation
        // In real implementation, this would create proper ISO format
        return String.format("MTI:0400|TransactionId:%s", input.internalTransactionId());
    }
}
