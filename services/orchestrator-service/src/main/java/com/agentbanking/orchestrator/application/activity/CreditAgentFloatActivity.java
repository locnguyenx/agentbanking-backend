package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCreditInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCreditResult;
import io.temporal.activity.ActivityInterface;




@ActivityInterface
public interface CreditAgentFloatActivity {
    FloatCreditResult creditAgentFloat(FloatCreditInput input);
}
