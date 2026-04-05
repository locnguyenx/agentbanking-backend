package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCommitInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCommitResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface CommitFloatActivity {
    FloatCommitResult commitFloat(FloatCommitInput input);
}
