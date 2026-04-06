package com.agentbanking.orchestrator.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TransactionResolutionCase(
    UUID id,
    UUID workflowId,
    UUID transactionId,
    ResolutionAction proposedAction,
    String reasonCode,
    String reason,
    String evidenceUrl,
    ResolutionStatus status,
    String makerUserId,
    Instant makerCreatedAt,
    String checkerUserId,
    String checkerAction,
    String checkerReason,
    Instant checkerCompletedAt,
    boolean temporalSignalSent,
    Instant createdAt,
    Instant updatedAt
) {
    public static TransactionResolutionCase createPendingMaker(
            UUID workflowId, UUID transactionId) {
        var now = Instant.now();
        return new TransactionResolutionCase(
            UUID.randomUUID(), workflowId, transactionId,
            null, null, null, null,
            ResolutionStatus.PENDING_MAKER,
            null, null,
            null, null, null, null,
            false, now, now
        );
    }

    public TransactionResolutionCase makerPropose(
            ResolutionAction action, String userId, String reasonCode,
            String reason, String evidenceUrl) {
        if (status != ResolutionStatus.PENDING_MAKER) {
            throw new IllegalStateException(
                "Cannot propose when status is " + status + ". Expected PENDING_MAKER.");
        }
        if (action == null || userId == null || reason == null) {
            throw new IllegalArgumentException("action, userId, and reason are required.");
        }
        var now = Instant.now();
        return new TransactionResolutionCase(
            id, workflowId, transactionId,
            action, reasonCode, reason, evidenceUrl,
            ResolutionStatus.PENDING_CHECKER,
            userId, now,
            checkerUserId, checkerAction, checkerReason, checkerCompletedAt,
            temporalSignalSent, createdAt, now
        );
    }

    public TransactionResolutionCase checkerApprove(String userId, String reason) {
        if (status != ResolutionStatus.PENDING_CHECKER) {
            throw new IllegalStateException(
                "Cannot approve when status is " + status + ". Expected PENDING_CHECKER.");
        }
        if (userId == null || reason == null) {
            throw new IllegalArgumentException("userId and reason are required.");
        }
        var now = Instant.now();
        return new TransactionResolutionCase(
            id, workflowId, transactionId,
            proposedAction, reasonCode, reason, evidenceUrl,
            ResolutionStatus.APPROVED,
            makerUserId, makerCreatedAt,
            userId, "APPROVED", reason, now,
            true, createdAt, now
        );
    }

    public TransactionResolutionCase checkerReject(String userId, String reason) {
        if (status != ResolutionStatus.PENDING_CHECKER) {
            throw new IllegalStateException(
                "Cannot reject when status is " + status + ". Expected PENDING_CHECKER.");
        }
        if (userId == null || reason == null) {
            throw new IllegalArgumentException("userId and reason are required.");
        }
        var now = Instant.now();
        return new TransactionResolutionCase(
            id, workflowId, transactionId,
            proposedAction, reasonCode, reason, evidenceUrl,
            ResolutionStatus.PENDING_MAKER,
            makerUserId, makerCreatedAt,
            userId, "REJECTED", reason, now,
            false, createdAt, now
        );
    }
}