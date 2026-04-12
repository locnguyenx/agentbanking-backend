package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.RequestToPayPort.RTPResult;




@ActivityInterface
public interface SendRequestToPayActivity {
    RTPResult send(String proxy, java.math.BigDecimal amount, String idempotencyKey);
}
