package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.BlockFloatActivity;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockResult;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@ActivityImpl(workers = "agent-banking-tasks")
@Component
public class BlockFloatActivityImpl implements BlockFloatActivity {

    private final LedgerServicePort ledgerServicePort;

    public BlockFloatActivityImpl(LedgerServicePort ledgerServicePort) {
        this.ledgerServicePort = ledgerServicePort;
    }

    @Override
    public FloatBlockResult blockFloat(FloatBlockInput input) {
        return ledgerServicePort.blockFloat(input);
    }
}
