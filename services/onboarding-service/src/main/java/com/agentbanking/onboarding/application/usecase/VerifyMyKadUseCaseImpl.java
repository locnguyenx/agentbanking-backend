package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.port.in.VerifyMyKadUseCase;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VerifyMyKadUseCaseImpl implements VerifyMyKadUseCase {

    @Override
    public VerifyMyKadResult verifyMyKad(String mykadNumber) {
        // Validate MyKad format (12 digits)
        if (mykadNumber == null || !mykadNumber.matches("^\\d{12}$")) {
            throw new IllegalArgumentException("MyKad must be exactly 12 digits");
        }

        // Simulate JPN verification (would call JPN API in production)
        UUID verificationId = UUID.randomUUID();
        return new VerifyMyKadResult(
            verificationId,
            "FOUND",
            "MOCK CUSTOMER",
            "1990-01-01",
            35,
            "CLEAN"
        );
    }
}