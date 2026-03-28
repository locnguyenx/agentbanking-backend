package com.agentbanking.biller.infrastructure.persistence.repository;

import com.agentbanking.biller.domain.model.EsspTransactionRecord;
import com.agentbanking.biller.domain.port.out.EsspTransactionRepository;
import com.agentbanking.biller.infrastructure.persistence.entity.EsspTransactionEntity;
import org.springframework.stereotype.Repository;

/**
 * JPA implementation of EsspTransactionRepository
 */
@Repository
public class EsspTransactionJpaRepository implements EsspTransactionRepository {

    @Override
    public EsspTransactionRecord save(EsspTransactionRecord transaction) {
        EsspTransactionEntity entity = toEntity(transaction);
        EsspTransactionEntity saved = saveEntity(entity);
        return toRecord(saved);
    }

    private EsspTransactionEntity toEntity(EsspTransactionRecord record) {
        EsspTransactionEntity entity = new EsspTransactionEntity();
        entity.setTransactionId(record.transactionId());
        entity.setInternalTransactionId(record.internalTransactionId());
        entity.setAmount(record.amount());
        entity.setStatus(record.status());
        entity.setEsspCertificateNumber(record.esspCertificateNumber());
        entity.setAgentReference(record.agentReference());
        entity.setCreatedAt(record.createdAt());
        entity.setCompletedAt(record.completedAt());
        return entity;
    }

    private EsspTransactionRecord toRecord(EsspTransactionEntity entity) {
        return new EsspTransactionRecord(
            entity.getTransactionId(),
            entity.getInternalTransactionId(),
            entity.getAmount(),
            entity.getStatus(),
            entity.getEsspCertificateNumber(),
            entity.getAgentReference(),
            entity.getCreatedAt(),
            entity.getCompletedAt()
        );
    }

    private EsspTransactionEntity saveEntity(EsspTransactionEntity entity) {
        // This would typically delegate to a Spring Data JPA repository
        // For now, we'll simulate the save operation
        // In a real implementation, this would be: return esspTransactionJpaRepository.save(entity);
        return entity; // Simplified for this example
    }
}