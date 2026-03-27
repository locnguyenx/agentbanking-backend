package com.agentbanking.ledger.infrastructure.persistence.mapper;

import com.agentbanking.ledger.domain.model.SettlementSummaryRecord;
import com.agentbanking.ledger.infrastructure.persistence.entity.SettlementSummaryEntity;

public class SettlementSummaryMapper {

    public static SettlementSummaryRecord toRecord(SettlementSummaryEntity entity) {
        if (entity == null) return null;
        return new SettlementSummaryRecord(
            entity.getSettlementId(),
            entity.getAgentId(),
            entity.getSettlementDate(),
            entity.getTotalWithdrawals(),
            entity.getTotalDeposits(),
            entity.getTotalBillPayments(),
            entity.getTotalRetailSales(),
            entity.getTotalCommissions(),
            entity.getNetAmount(),
            entity.getDirection(),
            entity.getCurrency(),
            entity.getGeneratedAt()
        );
    }

    public static SettlementSummaryEntity toEntity(SettlementSummaryRecord record) {
        if (record == null) return null;
        SettlementSummaryEntity entity = new SettlementSummaryEntity();
        entity.setSettlementId(record.settlementId());
        entity.setAgentId(record.agentId());
        entity.setSettlementDate(record.settlementDate());
        entity.setTotalWithdrawals(record.totalWithdrawals());
        entity.setTotalDeposits(record.totalDeposits());
        entity.setTotalBillPayments(record.totalBillPayments());
        entity.setTotalRetailSales(record.totalRetailSales());
        entity.setTotalCommissions(record.totalCommissions());
        entity.setNetAmount(record.netAmount());
        entity.setDirection(record.direction());
        entity.setCurrency(record.currency());
        entity.setGeneratedAt(record.generatedAt());
        return entity;
    }
}
