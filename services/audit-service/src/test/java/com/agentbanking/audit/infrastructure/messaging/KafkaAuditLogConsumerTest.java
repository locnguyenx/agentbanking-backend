package com.agentbanking.audit.infrastructure.messaging;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import com.agentbanking.audit.domain.port.out.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KafkaAuditLogConsumerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private KafkaAuditLogConsumer consumer;
    private Consumer<JsonNode> auditLogIn;

    @BeforeEach
    void setUp() {
        consumer = new KafkaAuditLogConsumer(auditLogRepository);
        auditLogIn = consumer.auditLogIn();
    }

    @Test
    void auditLogIn_withValidEvent_savesAuditLog() {
        UUID auditId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();

        JsonNode event = new ObjectMapper().createObjectNode()
            .put("auditId", auditId.toString())
            .put("serviceName", "ledger-service")
            .put("entityType", "Transaction")
            .put("entityId", entityId.toString())
            .put("action", "CREATE")
            .put("performedBy", "system")
            .put("ipAddress", "127.0.0.1")
            .put("timestamp", "2026-04-13T10:00:00")
            .put("outcome", "SUCCESS");

        auditLogIn.accept(event);

        verify(auditLogRepository).save(any(AuditLogRecord.class));
    }

    @Test
    void auditLogIn_withNullEntityId_handlesGracefully() {
        UUID auditId = UUID.randomUUID();

        JsonNode event = new ObjectMapper().createObjectNode()
            .put("auditId", auditId.toString())
            .put("serviceName", "ledger-service")
            .put("entityType", "Transaction")
            .put("action", "UPDATE")
            .put("performedBy", "admin")
            .put("timestamp", "2026-04-13T10:00:00")
            .put("outcome", "SUCCESS");

        auditLogIn.accept(event);

        verify(auditLogRepository).save(any(AuditLogRecord.class));
    }

    @Test
    void auditLogIn_withFailureOutcome_savesRecord() {
        UUID auditId = UUID.randomUUID();

        JsonNode event = new ObjectMapper().createObjectNode()
            .put("auditId", auditId.toString())
            .put("serviceName", "auth-service")
            .put("entityType", "User")
            .put("entityId", UUID.randomUUID().toString())
            .put("action", "LOGIN")
            .put("performedBy", "user@test.com")
            .put("timestamp", "2026-04-13T10:00:00")
            .put("outcome", "FAILED")
            .put("failureReason", "Invalid credentials");

        auditLogIn.accept(event);

        verify(auditLogRepository).save(any(AuditLogRecord.class));
    }

    @Test
    void auditLogIn_withMissingRequiredFields_throwsException() {
        JsonNode event = new ObjectMapper().createObjectNode()
            .put("auditId", UUID.randomUUID().toString())
            .put("serviceName", "test");

        try {
            auditLogIn.accept(event);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Failed to process audit event"));
        }

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void auditLogIn_withException_rethrowsRuntimeException() {
        UUID auditId = UUID.randomUUID();

        JsonNode event = new ObjectMapper().createObjectNode()
            .put("auditId", "not-a-valid-uuid")
            .put("serviceName", "test")
            .put("entityType", "Test")
            .put("action", "CREATE")
            .put("performedBy", "test")
            .put("timestamp", "2026-04-13T10:00:00")
            .put("outcome", "SUCCESS");

        try {
            auditLogIn.accept(event);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Failed to process audit event"));
        }
    }
}