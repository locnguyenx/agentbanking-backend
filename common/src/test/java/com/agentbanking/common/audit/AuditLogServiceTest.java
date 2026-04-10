package com.agentbanking.common.audit;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogServiceTest {

    @Test
    void shouldCreateAuditLogEntry() {
        UUID auditId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        
        AuditLogRecord record = new AuditLogRecord(
            auditId,
            "Agent",
            entityId,
            AuditAction.AGENT_CREATED,
            "admin@bank.com",
            "{\"name\": \"John Doe\"}",
            "192.168.1.1",
            LocalDateTime.now(),
            AuditOutcome.SUCCESS, null, null, null, "common", null, null
        );

        assertNotNull(record);
        assertEquals("Agent", record.entityType());
        assertEquals(entityId, record.entityId());
        assertEquals(AuditAction.AGENT_CREATED, record.action());
        assertEquals("admin@bank.com", record.performedBy());
    }

    @Test
    void shouldContainAllAuditActions() {
        AuditAction[] actions = AuditAction.values();
        
        assertEquals(52, actions.length);
        List<AuditAction> actionList = java.util.Arrays.asList(actions);
        assertTrue(actionList.contains(AuditAction.AGENT_CREATED));
        assertTrue(actionList.contains(AuditAction.WITHDRAWAL));
        assertTrue(actionList.contains(AuditAction.DEPOSIT));
    }

    @Test
    void shouldRejectNullAuditId() {
        assertThrows(NullPointerException.class, () -> {
            new AuditLogRecord(
                null,
                "Agent",
                UUID.randomUUID(),
                AuditAction.AGENT_CREATED,
                "admin@bank.com",
                "{}",
                "192.168.1.1",
                LocalDateTime.now(),
                AuditOutcome.SUCCESS, null, null, null, "common", null, null
            );
        });
    }

    @Test
    void shouldRejectNullEntityType() {
        assertThrows(NullPointerException.class, () -> {
            new AuditLogRecord(
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                AuditAction.AGENT_CREATED,
                "admin@bank.com",
                "{}",
                "192.168.1.1",
                LocalDateTime.now(),
                AuditOutcome.SUCCESS, null, null, null, "common", null, null
            );
        });
    }

    @Test
    void shouldRejectNullAction() {
        assertThrows(NullPointerException.class, () -> {
            new AuditLogRecord(
                UUID.randomUUID(),
                "Agent",
                UUID.randomUUID(),
                null,
                "admin@bank.com",
                "{}",
                "192.168.1.1",
                LocalDateTime.now(),
                AuditOutcome.SUCCESS, null, null, null, "common", null, null
            );
        });
    }

    @Test
    void shouldRejectNullPerformedBy() {
        assertThrows(NullPointerException.class, () -> {
            new AuditLogRecord(
                UUID.randomUUID(),
                "Agent",
                UUID.randomUUID(),
                AuditAction.AGENT_CREATED,
                null,
                "{}",
                "192.168.1.1",
                LocalDateTime.now(),
                AuditOutcome.SUCCESS, null, null, null, "common", null, null
            );
        });
    }
}