package com.agentbanking.onboarding.domain.port.out;

import com.agentbanking.onboarding.domain.model.KycStatus;
import com.agentbanking.onboarding.domain.model.KycVerificationRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KycVerificationRepository {
    List<KycVerificationRecord> findByVerificationStatusOrderByCreatedAtDesc(KycStatus status);
    KycVerificationRecord save(KycVerificationRecord verification);
    Optional<KycVerificationRecord> findById(UUID id);
}