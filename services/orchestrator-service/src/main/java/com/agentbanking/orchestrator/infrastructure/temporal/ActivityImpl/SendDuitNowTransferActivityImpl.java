package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.SendDuitNowTransferActivity;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.DuitNowTransferInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.DuitNowTransferResult;
import org.springframework.stereotype.Component;

@Component
public class SendDuitNowTransferActivityImpl implements SendDuitNowTransferActivity {

    private final SwitchAdapterPort switchAdapterPort;

    public SendDuitNowTransferActivityImpl(SwitchAdapterPort switchAdapterPort) {
        this.switchAdapterPort = switchAdapterPort;
    }

    @Override
    public DuitNowTransferResult sendDuitNowTransfer(DuitNowTransferInput input) {
        return switchAdapterPort.sendDuitNowTransfer(input);
    }
}
