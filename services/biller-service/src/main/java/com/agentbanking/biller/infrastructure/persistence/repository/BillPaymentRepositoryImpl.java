package com.agentbanking.biller.infrastructure.persistence.repository;

import com.agentbanking.biller.domain.model.BillPaymentRecord;
import com.agentbanking.biller.domain.port.out.BillPaymentRepository;
import com.agentbanking.biller.infrastructure.persistence.entity.BillPaymentEntity;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class BillPaymentRepositoryImpl implements BillPaymentRepository {

    private final BillPaymentJpaRepository jpaRepository;

    public BillPaymentRepositoryImpl(BillPaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public BillPaymentRecord save(BillPaymentRecord record) {
        BillPaymentEntity entity = toEntity(record);
        BillPaymentEntity saved = jpaRepository.save(entity);
        return toRecord(saved);
    }

    private BillPaymentRecord toRecord(BillPaymentEntity e) {
        return new BillPaymentRecord(
            e.getPaymentId(), e.getBillerId(), e.getInternalTransactionId(),
            e.getRef1(), e.getRef2(), e.getAmount(), e.getStatus(),
            e.getReceiptNo(), e.getBillerReference(), e.getCreatedAt(), e.getCompletedAt()
        );
    }

    private BillPaymentEntity toEntity(BillPaymentRecord r) {
        BillPaymentEntity e = new BillPaymentEntity();
        e.setPaymentId(r.paymentId());
        e.setBillerId(r.billerId());
        e.setInternalTransactionId(r.internalTransactionId());
        e.setRef1(r.ref1());
        e.setRef2(r.ref2());
        e.setAmount(r.amount());
        e.setStatus(r.status());
        e.setReceiptNo(r.receiptNo());
        e.setBillerReference(r.billerReference());
        e.setCreatedAt(r.createdAt());
        e.setCompletedAt(r.completedAt());
        return e;
    }
}