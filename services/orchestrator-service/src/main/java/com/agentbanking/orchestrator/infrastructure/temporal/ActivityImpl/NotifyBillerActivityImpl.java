package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.NotifyBillerActivity;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillNotificationInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillNotificationResult;


import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class NotifyBillerActivityImpl implements NotifyBillerActivity {

    private final BillerServicePort billerServicePort;

    public NotifyBillerActivityImpl(BillerServicePort billerServicePort) {
        this.billerServicePort = billerServicePort;
    }

    @Override
    public BillNotificationResult notifyBiller(BillNotificationInput input) {
        return billerServicePort.notifyBiller(input);
    }
}
