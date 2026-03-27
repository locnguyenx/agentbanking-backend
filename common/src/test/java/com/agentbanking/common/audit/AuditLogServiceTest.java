package com.agentbanking.common.audit;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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
            LocalDateTime.now()
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
        
        assertEquals(8, actions.length);
        assertArrayEquals(new AuditAction[]{
            AuditAction.AGENT_CREATED,
            AuditAction.AGENT_UPDATED,
            AuditAction.AGENT_DEACTIVATED,
            AuditAction.WITHDRAWAL,
            AuditAction.DEPOSIT,
            AuditAction.BILL_PAYMENT,
            AuditAction.TRANSACTION_COMMITTED,
            AuditAction.TRANSACTION_ROLLED_BACK
        }, actions);
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
                LocalDateTime.now()
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
                LocalDateTime.now()
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
                LocalDateTime.now()
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
                LocalDateTime.now()
            );
        });
    }
}