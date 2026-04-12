package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckResult;
import io.temporal.activity.ActivityInterface;




@ActivityInterface
public interface CheckVelocityActivity {
    VelocityCheckResult checkVelocity(VelocityCheckInput input);
}
