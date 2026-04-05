package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillNotificationInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillNotificationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface NotifyBillerActivity {
    BillNotificationResult notifyBiller(BillNotificationInput input);
}
