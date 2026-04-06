package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.TelcoAggregatorPort.TelcoValidationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ValidatePhoneNumberActivity {
    TelcoValidationResult validate(String phoneNumber, String telcoProvider);
}
