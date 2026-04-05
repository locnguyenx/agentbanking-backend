package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.AuthorizeAtSwitchActivity;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchAuthorizationInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchAuthorizationResult;
import org.springframework.stereotype.Component;

@Component
public class AuthorizeAtSwitchActivityImpl implements AuthorizeAtSwitchActivity {

    private final SwitchAdapterPort switchAdapterPort;

    public AuthorizeAtSwitchActivityImpl(SwitchAdapterPort switchAdapterPort) {
        this.switchAdapterPort = switchAdapterPort;
    }

    @Override
    public SwitchAuthorizationResult authorize(SwitchAuthorizationInput input) {
        return switchAdapterPort.authorizeTransaction(input);
    }
}
