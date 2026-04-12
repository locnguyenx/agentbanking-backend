package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.CheckVelocityActivity;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.VelocityCheckResult;


import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class CheckVelocityActivityImpl implements CheckVelocityActivity {

    private final RulesServicePort rulesServicePort;

    public CheckVelocityActivityImpl(RulesServicePort rulesServicePort) {
        this.rulesServicePort = rulesServicePort;
    }

    @Override
    public VelocityCheckResult checkVelocity(VelocityCheckInput input) {
        return rulesServicePort.checkVelocity(input);
    }
}
