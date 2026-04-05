package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.DuitNowTransferInput;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.DuitNowTransferResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface SendDuitNowTransferActivity {
    DuitNowTransferResult sendDuitNowTransfer(DuitNowTransferInput input);
}
