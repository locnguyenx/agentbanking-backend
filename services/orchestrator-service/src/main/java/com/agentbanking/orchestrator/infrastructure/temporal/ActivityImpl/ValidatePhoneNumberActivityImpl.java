package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.ValidatePhoneNumberActivity;
import com.agentbanking.orchestrator.domain.port.out.TelcoAggregatorPort;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@ActivityImpl(workers = "agent-banking-tasks")
@Component
public class ValidatePhoneNumberActivityImpl implements ValidatePhoneNumberActivity {

    private final TelcoAggregatorPort port;

    public ValidatePhoneNumberActivityImpl(TelcoAggregatorPort port) {
        this.port = port;
    }

    @Override
    public TelcoAggregatorPort.TelcoValidationResult validate(String phoneNumber, String telcoProvider) {
        return port.validatePhoneNumber(phoneNumber, telcoProvider);
    }
}
