package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.VerifyBiometricActivity;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@ActivityImpl(workers = "agent-banking-tasks")
@Component
public class VerifyBiometricActivityImpl implements VerifyBiometricActivity {

    @Override
    public BiometricResult verifyBiometric(String customerMykad) {
        return new BiometricResult(true, "VERIFIED", null);
    }
}
