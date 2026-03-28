package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DiscrepancyCase(
    UUID caseId,
    String transactionId,
    DiscrepancyType discrepancyType,
    BigDecimal internalAmount,
    BigDecimal networkAmount,
    DiscrepancyStatus status,
    String makerAction,
    String makerUserId,
    String makerReason,
    String checkerUserId,
    String checkerAction,
    String checkerReason,
    Instant createdAt,
    Instant resolvedAt
) {
    public DiscrepancyCase makerPropose(String action, String userId, String reason) {
        return new DiscrepancyCase(
            caseId, transactionId, discrepancyType,
            internalAmount, networkAmount,
            DiscrepancyStatus.PENDING_CHECKER,
            action, userId, reason,
            checkerUserId, checkerAction, checkerReason,
            createdAt, resolvedAt
        );
    }

    public DiscrepancyCase checkerApprove(String userId, String reason) {
        return new DiscrepancyCase(
            caseId, transactionId, discrepancyType,
            internalAmount, networkAmount,
            DiscrepancyStatus.RESOLVED,
            makerAction, makerUserId, makerReason,
            userId, "APPROVED", reason,
            createdAt, Instant.now()
        );
    }

    public DiscrepancyCase checkerReject(String userId, String reason) {
        return new DiscrepancyCase(
            caseId, transactionId, discrepancyType,
            internalAmount, networkAmount,
            DiscrepancyStatus.PENDING_MAKER,
            makerAction, makerUserId, makerReason,
            userId, "REJECTED", reason,
            createdAt, resolvedAt
        );
    }
}
