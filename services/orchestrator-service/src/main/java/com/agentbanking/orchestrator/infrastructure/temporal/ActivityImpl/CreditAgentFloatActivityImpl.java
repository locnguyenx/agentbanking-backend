package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.CreditAgentFloatActivity;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCreditInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCreditResult;


import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class CreditAgentFloatActivityImpl implements CreditAgentFloatActivity {

    private final LedgerServicePort ledgerServicePort;

    public CreditAgentFloatActivityImpl(LedgerServicePort ledgerServicePort) {
        this.ledgerServicePort = ledgerServicePort;
    }

    @Override
    public FloatCreditResult creditAgentFloat(FloatCreditInput input) {
        return ledgerServicePort.creditAgentFloat(input);
    }
}
