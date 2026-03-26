package com.agentbanking.onboarding.domain.port.in;

import java.util.List;

public interface GetReviewQueueUseCase {
    ReviewQueueResult getReviewQueue(int page, int size);

    record ReviewQueueResult(
        List<ReviewQueueItem> content,
        int totalElements,
        int totalPages,
        int page,
        int size
    ) {}

    record ReviewQueueItem(
        String verificationId,
        String mykadMasked,
        String fullName,
        String amlStatus,
        String biometricMatch,
        String rejectionReason
    ) {}
}