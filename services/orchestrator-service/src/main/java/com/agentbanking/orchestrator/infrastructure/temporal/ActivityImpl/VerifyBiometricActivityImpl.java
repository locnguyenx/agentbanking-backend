package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.VerifyBiometricActivity;
import org.springframework.stereotype.Component;

@Component
public class VerifyBiometricActivityImpl implements VerifyBiometricActivity {

    @Override
    public BiometricResult verifyBiometric(String customerMykad) {
        return new BiometricResult(true, "VERIFIED", null);
    }
}
