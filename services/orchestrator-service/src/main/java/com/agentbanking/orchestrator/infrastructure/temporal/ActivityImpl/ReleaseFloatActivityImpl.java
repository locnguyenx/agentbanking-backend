package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.ReleaseFloatActivity;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseResult;
import org.springframework.stereotype.Component;

@Component
public class ReleaseFloatActivityImpl implements ReleaseFloatActivity {

    private final LedgerServicePort ledgerServicePort;

    public ReleaseFloatActivityImpl(LedgerServicePort ledgerServicePort) {
        this.ledgerServicePort = ledgerServicePort;
    }

    @Override
    public FloatReleaseResult releaseFloat(FloatReleaseInput input) {
        return ledgerServicePort.releaseFloat(input);
    }
}
