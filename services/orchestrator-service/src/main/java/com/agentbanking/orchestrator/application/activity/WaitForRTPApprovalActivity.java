package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.RequestToPayPort.RTPStatus;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface WaitForRTPApprovalActivity {
    RTPStatus waitForApproval(String rtpReference, int timeoutSeconds);
}
