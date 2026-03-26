package com.agentbanking.onboarding.domain.port.in;

import java.util.Map;
import java.util.UUID;

public interface VerifyMyKadUseCase {
    VerifyMyKadResult verifyMyKad(String mykadNumber);

    record VerifyMyKadResult(
        UUID verificationId,
        String status,
        String fullName,
        String dateOfBirth,
        int age,
        String amlStatus
    ) {}
}