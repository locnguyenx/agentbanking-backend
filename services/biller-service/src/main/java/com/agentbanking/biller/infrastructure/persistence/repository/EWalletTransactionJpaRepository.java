package com.agentbanking.biller.infrastructure.persistence.repository;

import com.agentbanking.biller.domain.model.EWalletTransactionRecord;
import com.agentbanking.biller.domain.port.out.EWalletTransactionRepository;
import com.agentbanking.biller.infrastructure.persistence.entity.EWalletTransactionEntity;
import org.springframework.stereotype.Repository;

/**
 * JPA implementation of EWalletTransactionRepository
 */
@Repository
public class EWalletTransactionJpaRepository implements EWalletTransactionRepository {

    @Override
    public EWalletTransactionRecord save(EWalletTransactionRecord transaction) {
        EWalletTransactionEntity entity = toEntity(transaction);
        EWalletTransactionEntity saved = saveEntity(entity);
        return toRecord(saved);
    }

    private EWalletTransactionEntity toEntity(EWalletTransactionRecord record) {
        EWalletTransactionEntity entity = new EWalletTransactionEntity();
        entity.setTransactionId(record.transactionId());
        entity.setInternalTransactionId(record.internalTransactionId());
        entity.setWalletProvider(record.walletProvider());
        entity.setWalletId(record.walletId());
        entity.setAmount(record.amount());
        entity.setStatus(record.status());
        entity.setWalletReference(record.walletReference());
        entity.setAgentReference(record.agentReference());
        entity.setCreatedAt(record.createdAt());
        entity.setCompletedAt(record.completedAt());
        return entity;
    }

    private EWalletTransactionRecord toRecord(EWalletTransactionEntity entity) {
        return new EWalletTransactionRecord(
            entity.getTransactionId(),
            entity.getInternalTransactionId(),
            entity.getWalletProvider(),
            entity.getWalletId(),
            entity.getAmount(),
            entity.getStatus(),
            entity.getWalletReference(),
            entity.getAgentReference(),
            entity.getCreatedAt(),
            entity.getCompletedAt()
        );
    }

    private EWalletTransactionEntity saveEntity(EWalletTransactionEntity entity) {
        // This would typically delegate to a Spring Data JPA repository
        // For now, we'll simulate the save operation
        // In a real implementation, this would be: return eWalletTransactionJpaRepository.save(entity);
        return entity; // Simplified for this example
    }
}