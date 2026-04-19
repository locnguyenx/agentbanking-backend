package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.DailyMetricsResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.UUID;

@ActivityInterface
public interface GetDailyMetricsActivity {

    @ActivityMethod
    DailyMetricsResult getDailyMetrics(UUID agentId);
}
