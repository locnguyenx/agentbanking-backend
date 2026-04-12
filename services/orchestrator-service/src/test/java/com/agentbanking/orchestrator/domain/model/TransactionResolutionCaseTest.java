package com.agentbanking.orchestrator.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ResolutionActionTest {

    @Test
    @DisplayName("should have COMMIT and REVERSE values")
    void shouldHaveCorrectValues() {
        assertNotNull(ResolutionAction.COMMIT);
        assertNotNull(ResolutionAction.REVERSE);
        assertEquals(2, ResolutionAction.values().length);
    }
}

class ResolutionStatusTest {

    @Test
    @DisplayName("should have all required status values")
    void shouldHaveCorrectValues() {
        assertNotNull(ResolutionStatus.PENDING_MAKER);
        assertNotNull(ResolutionStatus.PENDING_CHECKER);
        assertNotNull(ResolutionStatus.APPROVED);
        assertNotNull(ResolutionStatus.REJECTED);
        assertEquals(4, ResolutionStatus.values().length);
    }
}

class TransactionResolutionCaseTest {

    @Nested
    @DisplayName("createPendingMaker")
    class CreatePendingMaker {
        @Test
        @DisplayName("should create case with PENDING_MAKER status")
        void shouldCreateWithPendingMakerStatus() {
            UUID workflowId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();

            var result = TransactionResolutionCase.createPendingMaker(workflowId, transactionId, "AWAITING_REVIEW");

            assertNotNull(result.id());
            assertEquals(workflowId, result.workflowId());
            assertEquals(transactionId, result.transactionId());
            assertEquals(ResolutionStatus.PENDING_MAKER, result.status());
            assertNull(result.proposedAction());
            assertNull(result.makerUserId());
            assertNull(result.checkerUserId());
            assertNotNull(result.createdAt());
            assertNotNull(result.updatedAt());
        }
    }

    @Nested
    @DisplayName("makerPropose")
    class MakerPropose {
        @Test
        @DisplayName("should transition to PENDING_CHECKER status")
        void shouldTransitionToPendingChecker() {
            var existing = createSampleCase(ResolutionStatus.PENDING_MAKER);
            String userId = "maker-001";
            ResolutionAction action = ResolutionAction.COMMIT;
            String reasonCode = "TIMEOUT";
            String reason = "Transaction timed out waiting for response";
            String evidenceUrl = "https://storage/evidence/123";

            var result = existing.makerPropose(action, userId, reasonCode, reason, evidenceUrl);

            assertEquals(existing.id(), result.id());
            assertEquals(ResolutionStatus.PENDING_CHECKER, result.status());
            assertEquals(action, result.proposedAction());
            assertEquals(reasonCode, result.reasonCode());
            assertEquals(reason, result.reason());
            assertEquals(evidenceUrl, result.evidenceUrl());
            assertEquals(userId, result.makerUserId());
            assertNotNull(result.makerCreatedAt());
        }
    }

    @Nested
    @DisplayName("checkerApprove")
    class CheckerApprove {
        @Test
        @DisplayName("should transition to APPROVED status")
        void shouldTransitionToApproved() {
            var existing = createSampleCase(ResolutionStatus.PENDING_CHECKER);
            String userId = "checker-001";
            String reason = "Approved after review";

            var result = existing.checkerApprove(userId, reason);

            assertEquals(ResolutionStatus.APPROVED, result.status());
            assertEquals(userId, result.checkerUserId());
            assertEquals("APPROVED", result.checkerAction());
            assertEquals(reason, result.checkerReason());
            assertNotNull(result.checkerCompletedAt());
            assertTrue(result.temporalSignalSent());
        }
    }

    @Nested
    @DisplayName("checkerReject")
    class CheckerReject {
        @Test
        @DisplayName("should transition back to PENDING_MAKER status")
        void shouldTransitionBackToPendingMaker() {
            var existing = createSampleCase(ResolutionStatus.PENDING_CHECKER);
            String userId = "checker-001";
            String reason = "Missing evidence";

            var result = existing.checkerReject(userId, reason);

            assertEquals(ResolutionStatus.PENDING_MAKER, result.status());
            assertEquals(userId, result.checkerUserId());
            assertEquals("REJECTED", result.checkerAction());
            assertEquals(reason, result.checkerReason());
            assertNotNull(result.checkerCompletedAt());
            assertFalse(result.temporalSignalSent());
        }
    }

    private TransactionResolutionCase createSampleCase(ResolutionStatus status) {
        var now = Instant.now();
        return new TransactionResolutionCase(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            ResolutionAction.COMMIT,
            "CODE",
            "reason",
            "url",
            status,
            "maker",
            now,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            now,
            now
        );
    }
}