package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchAuthorizationInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchAuthorizationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface AuthorizeAtSwitchActivity {
    SwitchAuthorizationResult authorize(SwitchAuthorizationInput input);
}
