package com.agentbanking.ledger.infrastructure.persistence.mapper;

import com.agentbanking.ledger.domain.model.DiscrepancyCase;
import com.agentbanking.ledger.infrastructure.persistence.entity.DiscrepancyCaseEntity;

public class DiscrepancyCaseMapper {

    public static DiscrepancyCaseEntity toEntity(DiscrepancyCase record) {
        DiscrepancyCaseEntity entity = new DiscrepancyCaseEntity();
        entity.setCaseId(record.caseId());
        entity.setTransactionId(record.transactionId());
        entity.setDiscrepancyType(record.discrepancyType());
        entity.setInternalAmount(record.internalAmount());
        entity.setNetworkAmount(record.networkAmount());
        entity.setStatus(record.status());
        entity.setMakerAction(record.makerAction());
        entity.setMakerUserId(record.makerUserId());
        entity.setMakerReason(record.makerReason());
        entity.setCheckerUserId(record.checkerUserId());
        entity.setCheckerAction(record.checkerAction());
        entity.setCheckerReason(record.checkerReason());
        entity.setCreatedAt(record.createdAt());
        entity.setResolvedAt(record.resolvedAt());
        return entity;
    }

    public static DiscrepancyCase toRecord(DiscrepancyCaseEntity entity) {
        return new DiscrepancyCase(
                entity.getCaseId(),
                entity.getTransactionId(),
                entity.getDiscrepancyType(),
                entity.getInternalAmount(),
                entity.getNetworkAmount(),
                entity.getStatus(),
                entity.getMakerAction(),
                entity.getMakerUserId(),
                entity.getMakerReason(),
                entity.getCheckerUserId(),
                entity.getCheckerAction(),
                entity.getCheckerReason(),
                entity.getCreatedAt(),
                entity.getResolvedAt()
        );
    }
}
