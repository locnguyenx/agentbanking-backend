package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseResult;
import io.temporal.activity.ActivityInterface;




@ActivityInterface
public interface ReleaseFloatActivity {
    FloatReleaseResult releaseFloat(FloatReleaseInput input);
}
