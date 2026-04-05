package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.CalculateFeesActivity;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationResult;
import org.springframework.stereotype.Component;

@Component
public class CalculateFeesActivityImpl implements CalculateFeesActivity {

    private final RulesServicePort rulesServicePort;

    public CalculateFeesActivityImpl(RulesServicePort rulesServicePort) {
        this.rulesServicePort = rulesServicePort;
    }

    @Override
    public FeeCalculationResult calculateFees(FeeCalculationInput input) {
        return rulesServicePort.calculateFees(input);
    }
}
