package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.RequestToPayPort.RTPStatus;




@ActivityInterface
public interface WaitForRTPApprovalActivity {
    RTPStatus waitForApproval(String rtpReference, int timeoutSeconds);
}
