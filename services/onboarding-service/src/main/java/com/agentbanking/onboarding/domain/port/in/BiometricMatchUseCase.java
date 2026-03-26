package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.BiometricResult;

public interface BiometricMatchUseCase {
    BiometricMatchResult matchBiometric(String verificationId, String biometricData);

    record BiometricMatchResult(
        String verificationId,
        String status,
        String biometricMatch,
        String timestamp
    ) {}
}