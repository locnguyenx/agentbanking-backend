package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.SendReversalToSwitchActivity;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.SwitchReversalResult;


import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class SendReversalToSwitchActivityImpl implements SendReversalToSwitchActivity {

    private final SwitchAdapterPort switchAdapterPort;

    public SendReversalToSwitchActivityImpl(SwitchAdapterPort switchAdapterPort) {
        this.switchAdapterPort = switchAdapterPort;
    }

    @Override
    public SwitchReversalResult sendReversal(SwitchReversalInput input) {
        return switchAdapterPort.sendReversal(input);
    }
}
