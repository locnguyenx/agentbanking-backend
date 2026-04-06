package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.RequestToPayPort.RTPResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface SendRequestToPayActivity {
    RTPResult send(String proxy, java.math.BigDecimal amount, String idempotencyKey);
}
