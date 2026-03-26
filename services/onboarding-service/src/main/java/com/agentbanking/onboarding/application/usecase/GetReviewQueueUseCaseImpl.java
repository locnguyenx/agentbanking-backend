package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.model.KycStatus;
import com.agentbanking.onboarding.domain.model.KycVerificationRecord;
import com.agentbanking.onboarding.domain.port.in.GetReviewQueueUseCase;
import com.agentbanking.onboarding.domain.port.out.KycVerificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetReviewQueueUseCaseImpl implements GetReviewQueueUseCase {

    private final KycVerificationRepository kycVerificationRepository;

    public GetReviewQueueUseCaseImpl(KycVerificationRepository kycVerificationRepository) {
        this.kycVerificationRepository = kycVerificationRepository;
    }

    @Override
    public ReviewQueueResult getReviewQueue(int page, int size) {
        List<KycVerificationRecord> pending = kycVerificationRepository
            .findByVerificationStatusOrderByCreatedAtDesc(KycStatus.MANUAL_REVIEW);

        List<ReviewQueueItem> content = pending.stream()
            .map(k -> new ReviewQueueItem(
                k.verificationId().toString(),
                maskMykad(k.mykadNumber()),
                k.fullName() != null ? k.fullName() : "",
                k.amlStatus() != null ? k.amlStatus().name() : "UNKNOWN",
                k.biometricMatch() != null ? k.biometricMatch().name() : "UNKNOWN",
                k.rejectionReason() != null ? k.rejectionReason() : ""
            ))
            .toList();

        return new ReviewQueueResult(
            content,
            pending.size(),
            1,
            page,
            size
        );
    }

    private String maskMykad(String mykad) {
        if (mykad == null || mykad.length() < 4) return "****";
        return mykad.substring(0, 4) + "********";
    }
}