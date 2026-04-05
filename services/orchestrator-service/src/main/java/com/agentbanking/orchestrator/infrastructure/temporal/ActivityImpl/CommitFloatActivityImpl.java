package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.CommitFloatActivity;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCommitInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCommitResult;
import org.springframework.stereotype.Component;

@Component
public class CommitFloatActivityImpl implements CommitFloatActivity {

    private final LedgerServicePort ledgerServicePort;

    public CommitFloatActivityImpl(LedgerServicePort ledgerServicePort) {
        this.ledgerServicePort = ledgerServicePort;
    }

    @Override
    public FloatCommitResult commitFloat(FloatCommitInput input) {
        return ledgerServicePort.commitFloat(input);
    }
}
