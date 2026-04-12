package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationInput;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.FeeCalculationResult;
import io.temporal.activity.ActivityInterface;




@ActivityInterface
public interface CalculateFeesActivity {
    FeeCalculationResult calculateFees(FeeCalculationInput input);
}
