package com.agentbanking.switchadapter.infrastructure.persistence.repository;

import com.agentbanking.switchadapter.domain.model.MessageType;
import com.agentbanking.switchadapter.domain.model.SwitchStatus;
import com.agentbanking.switchadapter.domain.model.SwitchTransactionRecord;
import com.agentbanking.switchadapter.domain.port.out.SwitchTransactionRepository;
import com.agentbanking.switchadapter.infrastructure.persistence.entity.SwitchTransactionEntity;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JpaSwitchTransactionRepository implements SwitchTransactionRepository {

    @Override
    public void save(SwitchTransactionRecord record) {
        SwitchTransactionEntity entity = toEntity(record);
    }

    @Override
    public SwitchTransactionRecord findById(UUID switchTxId) {
        return null;
    }

    private SwitchTransactionEntity toEntity(SwitchTransactionRecord record) {
        SwitchTransactionEntity entity = new SwitchTransactionEntity();
        entity.setSwitchTxId(record.switchTxId());
        entity.setInternalTransactionId(record.internalTransactionId());
        entity.setMtType(record.mtType());
        entity.setIsoResponseCode(record.isoResponseCode());
        entity.setSwitchReference(record.switchReference());
        entity.setStatus(record.status());
        entity.setOriginalReference(record.originalReference());
        entity.setReversalCount(record.reversalCount());
        entity.setAmount(record.amount());
        entity.setCreatedAt(record.createdAt());
        entity.setCompletedAt(record.completedAt());
        return entity;
    }
}
