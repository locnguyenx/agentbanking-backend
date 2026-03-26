package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.domain.model.KycStatus;
import com.agentbanking.onboarding.domain.model.KycVerificationRecord;
import com.agentbanking.onboarding.domain.port.out.KycVerificationRepository;
import com.agentbanking.onboarding.infrastructure.persistence.entity.KycVerificationEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class KycVerificationRepositoryImpl implements KycVerificationRepository {

    private final KycVerificationJpaRepository jpaRepository;

    public KycVerificationRepositoryImpl(KycVerificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<KycVerificationRecord> findByVerificationStatusOrderByCreatedAtDesc(KycStatus status) {
        return jpaRepository.findByVerificationStatusOrderByCreatedAtDesc(status)
            .stream()
            .map(this::toRecord)
            .toList();
    }

    @Override
    public KycVerificationRecord save(KycVerificationRecord record) {
        KycVerificationEntity entity = toEntity(record);
        KycVerificationEntity saved = jpaRepository.save(entity);
        return toRecord(saved);
    }

    @Override
    public Optional<KycVerificationRecord> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toRecord);
    }

    private KycVerificationRecord toRecord(KycVerificationEntity entity) {
        return new KycVerificationRecord(
            entity.getVerificationId(),
            entity.getMykadNumber(),
            entity.getFullName(),
            entity.getDateOfBirth(),
            entity.getAge(),
            entity.getAmlStatus(),
            entity.getBiometricMatch(),
            entity.getVerificationStatus(),
            entity.getRejectionReason(),
            entity.getVerifiedAt(),
            entity.getReviewedBy(),
            entity.getCreatedAt()
        );
    }

    private KycVerificationEntity toEntity(KycVerificationRecord record) {
        KycVerificationEntity entity = new KycVerificationEntity();
        entity.setVerificationId(record.verificationId());
        entity.setMykadNumber(record.mykadNumber());
        entity.setFullName(record.fullName());
        entity.setDateOfBirth(record.dateOfBirth());
        entity.setAge(record.age());
        entity.setAmlStatus(record.amlStatus());
        entity.setBiometricMatch(record.biometricMatch());
        entity.setVerificationStatus(record.verificationStatus());
        entity.setRejectionReason(record.rejectionReason());
        entity.setVerifiedAt(record.verifiedAt());
        entity.setReviewedBy(record.reviewedBy());
        entity.setCreatedAt(record.createdAt());
        return entity;
    }
}