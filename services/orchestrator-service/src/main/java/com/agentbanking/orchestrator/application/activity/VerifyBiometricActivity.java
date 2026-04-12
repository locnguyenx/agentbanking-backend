package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;





@ActivityInterface
public interface VerifyBiometricActivity {
    BiometricResult verifyBiometric(String customerMykad);

    record BiometricResult(boolean match, String status, String errorCode) {}
}
