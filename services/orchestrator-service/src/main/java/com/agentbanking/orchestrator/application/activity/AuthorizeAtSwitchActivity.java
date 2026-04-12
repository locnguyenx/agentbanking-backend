package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchAuthorizationInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchAuthorizationResult;




@ActivityInterface
public interface AuthorizeAtSwitchActivity {
    SwitchAuthorizationResult authorize(SwitchAuthorizationInput input);
}
