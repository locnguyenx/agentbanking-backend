package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.common.audit.AuditAction;
import com.agentbanking.common.audit.AuditLogRecord;
import com.agentbanking.common.audit.AuditOutcome;
import com.agentbanking.onboarding.domain.port.out.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(auditLogRepository);
    }

    @Test
    void shouldSaveAuditLogRecord() {
        UUID auditId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.now();

        AuditLogRecord inputRecord = new AuditLogRecord(
            auditId,
            "Agent",
            entityId,
            AuditAction.AGENT_CREATED,
            "admin@bank.com",
            "{\"name\": \"John Doe\"}",
            "192.168.1.1",
            timestamp,
            AuditOutcome.SUCCESS,
            null, // failureReason
            "trace-123", // traceId
            "session-456", // sessionId
            "onboarding-service", // serviceName
            "web", // deviceInfo
            "KL" // geographicLocation
        );

        when(auditLogRepository.save(any(AuditLogRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLogRecord result = auditLogService.log(inputRecord);

        assertNotNull(result);
        assertEquals(auditId, result.auditId());
        assertEquals("Agent", result.entityType());
        assertEquals(entityId, result.entityId());
        assertEquals(AuditAction.AGENT_CREATED, result.action());
        assertEquals("admin@bank.com", result.performedBy());
    }

    @Test
    void shouldGenerateTimestampWhenNotProvided() {
        UUID auditId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();

        AuditLogRecord inputRecord = new AuditLogRecord(
            auditId,
            "Agent",
            entityId,
            AuditAction.AGENT_UPDATED,
            "admin@bank.com",
            "{\"name\": \"Jane Doe\"}",
            "10.0.0.1",
            LocalDateTime.now(),
            AuditOutcome.SUCCESS,
            null, // failureReason
            "trace-456", // traceId
            "session-789", // sessionId
            "onboarding-service", // serviceName
            "web", // deviceInfo
            "KL" // geographicLocation
        );

        when(auditLogRepository.save(any(AuditLogRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLogRecord result = auditLogService.log(inputRecord);

        assertNotNull(result);
        assertNotNull(result.timestamp());
    }

    @Test
    void shouldCallRepositoryWithAllFields() {
        UUID auditId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        AuditLogRecord inputRecord = new AuditLogRecord(
            auditId,
            "Transaction",
            entityId,
            AuditAction.DEPOSIT,
            "agent@bank.com",
            "{\"amount\": 1000.00, \"currency\": \"MYR\"}",
            "192.168.1.100",
            timestamp,
            AuditOutcome.SUCCESS,
            null, // failureReason
            "trace-789", // traceId
            "session-012", // sessionId
            "onboarding-service", // serviceName
            "mobile", // deviceInfo
            "Penang" // geographicLocation
        );

        when(auditLogRepository.save(any(AuditLogRecord.class))).thenReturn(inputRecord);

        AuditLogRecord result = auditLogService.log(inputRecord);

        assertEquals(auditId, result.auditId());
        assertEquals("Transaction", result.entityType());
        assertEquals(entityId, result.entityId());
        assertEquals(AuditAction.DEPOSIT, result.action());
        assertEquals("agent@bank.com", result.performedBy());
        assertEquals("{\"amount\": 1000.00, \"currency\": \"MYR\"}", result.changes());
        assertEquals("192.168.1.100", result.ipAddress());
        assertEquals(timestamp, result.timestamp());
    }

    @Test
    void shouldLogTransactionEvents() {
        UUID auditId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();

        AuditLogRecord withdrawalRecord = new AuditLogRecord(
            auditId,
            "Transaction",
            entityId,
            AuditAction.WITHDRAWAL,
            "agent@bank.com",
            "{\"amount\": 500.00}",
            "10.0.0.50",
            LocalDateTime.now(),
            AuditOutcome.SUCCESS,
            null, // failureReason
            "trace-wdl", // traceId
            "session-wdl", // sessionId
            "onboarding-service", // serviceName
            "pos", // deviceInfo
            "Johor" // geographicLocation
        );

        AuditLogRecord billPaymentRecord = new AuditLogRecord(
            UUID.randomUUID(),
            "Transaction",
            UUID.randomUUID(),
            AuditAction.BILL_PAYMENT,
            "agent@bank.com",
            "{\"biller\": \"TM\", \"amount\": 150.00}",
            "10.0.0.50",
            LocalDateTime.now(),
            AuditOutcome.SUCCESS,
            null, // failureReason
            "trace-bill", // traceId
            "session-bill", // sessionId
            "onboarding-service", // serviceName
            "mobile", // deviceInfo
            "Selangor" // geographicLocation
        );

        AuditLogRecord committedRecord = new AuditLogRecord(
            UUID.randomUUID(),
            "Transaction",
            UUID.randomUUID(),
            AuditAction.TRANSACTION_COMMITTED,
            "system",
            "{\"status\": \"COMMITTED\"}",
            "127.0.0.1", // ipAddress
            LocalDateTime.now(),
            AuditOutcome.SUCCESS,
            null, // failureReason
            "trace-commit", // traceId
            "session-commit", // sessionId
            "onboarding-service", // serviceName
            "system", // deviceInfo
            "DC" // geographicLocation
        );

        AuditLogRecord rolledBackRecord = new AuditLogRecord(
            UUID.randomUUID(),
            "Transaction",
            UUID.randomUUID(),
            AuditAction.TRANSACTION_ROLLED_BACK,
            "system",
            "{\"status\": \"ROLLED_BACK\", \"reason\": \"timeout\"}",
            "127.0.0.1", // ipAddress
            LocalDateTime.now(),
            AuditOutcome.FAILURE,
            "timeout", // failureReason
            "trace-rollback", // traceId
            "session-rollback", // sessionId
            "onboarding-service", // serviceName
            "system", // deviceInfo
            "DC" // geographicLocation
        );

        when(auditLogRepository.save(any(AuditLogRecord.class)))
            .thenReturn(withdrawalRecord)
            .thenReturn(billPaymentRecord)
            .thenReturn(committedRecord)
            .thenReturn(rolledBackRecord);

        assertNotNull(auditLogService.log(withdrawalRecord));
        assertNotNull(auditLogService.log(billPaymentRecord));
        assertNotNull(auditLogService.log(committedRecord));
        assertNotNull(auditLogService.log(rolledBackRecord));

        verify(auditLogRepository, times(4)).save(any(AuditLogRecord.class));
    }
}
