package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockResult;
import io.temporal.activity.ActivityInterface;




@ActivityInterface
public interface BlockFloatActivity {
    FloatBlockResult blockFloat(FloatBlockInput input);
}
