package com.agentbanking.biller.infrastructure.persistence.repository;

import com.agentbanking.biller.domain.model.TopupTransactionRecord;
import com.agentbanking.biller.domain.port.out.TopupTransactionRepository;
import com.agentbanking.biller.infrastructure.persistence.entity.TopupTransactionEntity;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class TopupTransactionRepositoryImpl implements TopupTransactionRepository {

    private final TopupTransactionJpaRepository jpaRepository;

    public TopupTransactionRepositoryImpl(TopupTransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TopupTransactionRecord save(TopupTransactionRecord record) {
        TopupTransactionEntity entity = toEntity(record);
        TopupTransactionEntity saved = jpaRepository.save(entity);
        return toRecord(saved);
    }

    private TopupTransactionRecord toRecord(TopupTransactionEntity e) {
        return new TopupTransactionRecord(
            e.getTopupId(), e.getInternalTransactionId(), e.getTelco(),
            e.getPhoneNumber(), e.getAmount(), e.getStatus(),
            e.getTelcoReference(), e.getCreatedAt(), e.getCompletedAt()
        );
    }

    private TopupTransactionEntity toEntity(TopupTransactionRecord r) {
        TopupTransactionEntity e = new TopupTransactionEntity();
        e.setTopupId(r.topupId());
        e.setInternalTransactionId(r.internalTransactionId());
        e.setTelco(r.telco());
        e.setPhoneNumber(r.phoneNumber());
        e.setAmount(r.amount());
        e.setStatus(r.status());
        e.setTelcoReference(r.telcoReference());
        e.setCreatedAt(r.createdAt());
        e.setCompletedAt(r.completedAt());
        return e;
    }
}