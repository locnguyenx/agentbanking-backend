package com.agentbanking.ledger.infrastructure.persistence.mapper;

import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.ledger.domain.model.TransactionStatus;
import com.agentbanking.ledger.domain.model.TransactionType;
import com.agentbanking.ledger.infrastructure.persistence.entity.TransactionEntity;

public class TransactionMapper {
    
    public static TransactionRecord toRecord(TransactionEntity entity) {
        if (entity == null) return null;
        return new TransactionRecord(
            entity.getTransactionId(),
            entity.getIdempotencyKey(),
            entity.getAgentId(),
            entity.getTransactionType(),
            entity.getAmount(),
            entity.getCustomerFee(),
            entity.getAgentCommission(),
            entity.getBankShare(),
            entity.getStatus(),
            entity.getErrorCode(),
            entity.getCustomerMykad(),
            entity.getCustomerCardMasked(),
            entity.getSwitchReference(),
            entity.getGeofenceLat(),
            entity.getGeofenceLng(),
            entity.getCreatedAt(),
            entity.getCompletedAt()
        );
    }
    
    public static TransactionEntity toEntity(TransactionRecord record) {
        if (record == null) return null;
        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId(record.transactionId());
        entity.setIdempotencyKey(record.idempotencyKey());
        entity.setAgentId(record.agentId());
        entity.setTransactionType(record.transactionType());
        entity.setAmount(record.amount());
        entity.setCustomerFee(record.customerFee());
        entity.setAgentCommission(record.agentCommission());
        entity.setBankShare(record.bankShare());
        entity.setStatus(record.status());
        entity.setErrorCode(record.errorCode());
        entity.setCustomerMykad(record.customerMykad());
        entity.setCustomerCardMasked(record.customerCardMasked());
        entity.setSwitchReference(record.switchReference());
        entity.setGeofenceLat(record.geofenceLat());
        entity.setGeofenceLng(record.geofenceLng());
        entity.setCreatedAt(record.createdAt());
        entity.setCompletedAt(record.completedAt());
        return entity;
    }
}
