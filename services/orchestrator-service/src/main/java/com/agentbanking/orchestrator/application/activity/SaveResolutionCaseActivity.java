package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;


import io.temporal.activity.ActivityMethod;
import java.util.UUID;



@ActivityInterface
public interface SaveResolutionCaseActivity {

    record Input(
        String workflowId,
        UUID transactionId,
        String reasonCode,
        String reason
    ) {}

    @ActivityMethod
    void saveResolutionCase(Input input);
}