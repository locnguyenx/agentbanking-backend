package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillNotificationInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillNotificationResult;




@ActivityInterface
public interface NotifyBillerActivity {
    BillNotificationResult notifyBiller(BillNotificationInput input);
}
