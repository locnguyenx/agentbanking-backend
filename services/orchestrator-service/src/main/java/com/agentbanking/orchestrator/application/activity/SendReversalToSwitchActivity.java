package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface SendReversalToSwitchActivity {
    SwitchReversalResult sendReversal(SwitchReversalInput input);
}
