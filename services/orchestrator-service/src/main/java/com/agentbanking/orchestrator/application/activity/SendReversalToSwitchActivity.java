package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalResult;



@ActivityInterface
public interface SendReversalToSwitchActivity {
    @ActivityMethod(name = "SendReversalToSwitch")
    SwitchReversalResult sendReversal(SwitchReversalInput input);

    // Enhanced method with retry logic for Safety Reversal (BDD-SR)
    @ActivityMethod(name = "SendReversalWithRetry")
    SafetyReversalResult sendReversalWithRetry(SwitchReversalInput input);

    record SafetyReversalResult(
        boolean success,
        String errorCode,
        int retryCount,
        boolean flaggedForManualIntervention
    ) {}
}
