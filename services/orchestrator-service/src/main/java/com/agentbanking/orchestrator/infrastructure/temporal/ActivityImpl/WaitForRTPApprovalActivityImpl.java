package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.WaitForRTPApprovalActivity;
import com.agentbanking.orchestrator.domain.port.out.RequestToPayPort;


import org.springframework.stereotype.Component;

@Component
public class WaitForRTPApprovalActivityImpl implements WaitForRTPApprovalActivity {

    private final RequestToPayPort port;

    public WaitForRTPApprovalActivityImpl(RequestToPayPort port) {
        this.port = port;
    }

    @Override
    public RequestToPayPort.RTPStatus waitForApproval(String rtpReference, int timeoutSeconds) {
        return port.checkRTPStatus(rtpReference);
    }
}
