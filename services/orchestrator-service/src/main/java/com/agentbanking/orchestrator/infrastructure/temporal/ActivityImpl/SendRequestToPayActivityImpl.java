package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.SendRequestToPayActivity;
import com.agentbanking.orchestrator.domain.port.out.RequestToPayPort;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class SendRequestToPayActivityImpl implements SendRequestToPayActivity {

    private final RequestToPayPort port;

    public SendRequestToPayActivityImpl(RequestToPayPort port) {
        this.port = port;
    }

    @Override
    public RequestToPayPort.RTPResult send(String proxy, BigDecimal amount, String idempotencyKey) {
        return port.sendRequestToPay(proxy, amount, idempotencyKey);
    }
}
