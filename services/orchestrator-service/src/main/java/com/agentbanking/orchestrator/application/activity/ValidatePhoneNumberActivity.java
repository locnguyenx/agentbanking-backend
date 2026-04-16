package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import com.agentbanking.orchestrator.domain.port.out.TelcoAggregatorPort.TelcoValidationResult;



@ActivityInterface
public interface ValidatePhoneNumberActivity {
    @ActivityMethod(name = "ValidatePhoneNumber")
    TelcoValidationResult validate(String phoneNumber, String telcoProvider);
}
