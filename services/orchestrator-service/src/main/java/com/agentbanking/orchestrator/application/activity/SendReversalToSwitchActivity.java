package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalResult;




@ActivityInterface
public interface SendReversalToSwitchActivity {
    SwitchReversalResult sendReversal(SwitchReversalInput input);
}
