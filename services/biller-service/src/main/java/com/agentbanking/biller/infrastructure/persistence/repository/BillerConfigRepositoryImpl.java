package com.agentbanking.biller.infrastructure.persistence.repository;

import com.agentbanking.biller.domain.model.BillerConfigRecord;
import com.agentbanking.biller.domain.port.out.BillerConfigRepository;
import com.agentbanking.biller.infrastructure.persistence.entity.BillerConfigEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class BillerConfigRepositoryImpl implements BillerConfigRepository {

    private final BillerConfigJpaRepository jpaRepository;

    public BillerConfigRepositoryImpl(BillerConfigJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<BillerConfigRecord> findByBillerCodeAndActiveTrue(String billerCode) {
        return jpaRepository.findByBillerCodeAndActiveTrue(billerCode).map(this::toRecord);
    }

    @Override
    public BillerConfigRecord save(BillerConfigRecord record) {
        BillerConfigEntity entity = toEntity(record);
        BillerConfigEntity saved = jpaRepository.save(entity);
        return toRecord(saved);
    }

    private BillerConfigRecord toRecord(BillerConfigEntity e) {
        return new BillerConfigRecord(
            e.getBillerId(), e.getBillerCode(), e.getBillerName(),
            e.getBillerType(), e.getApiEndpoint(), e.isActive(), e.getCreatedAt()
        );
    }

    private BillerConfigEntity toEntity(BillerConfigRecord r) {
        BillerConfigEntity e = new BillerConfigEntity();
        e.setBillerId(r.billerId());
        e.setBillerCode(r.billerCode());
        e.setBillerName(r.billerName());
        e.setBillerType(r.billerType());
        e.setApiEndpoint(r.apiEndpoint());
        e.setActive(r.active());
        e.setCreatedAt(r.createdAt());
        return e;
    }
}