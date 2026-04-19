package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.GetDailyMetricsActivity;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.DailyMetricsResult;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class GetDailyMetricsActivityImpl implements GetDailyMetricsActivity {

    private final LedgerServicePort ledgerServicePort;

    public GetDailyMetricsActivityImpl(LedgerServicePort ledgerServicePort) {
        this.ledgerServicePort = ledgerServicePort;
    }

    @Override
    public DailyMetricsResult getDailyMetrics(UUID agentId) {
        return ledgerServicePort.getDailyMetrics(agentId);
    }
}
