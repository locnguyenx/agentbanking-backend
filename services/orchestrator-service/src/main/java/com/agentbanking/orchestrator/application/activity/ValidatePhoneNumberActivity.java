package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import com.agentbanking.orchestrator.domain.port.out.TelcoAggregatorPort.TelcoValidationResult;




@ActivityInterface
public interface ValidatePhoneNumberActivity {
    TelcoValidationResult validate(String phoneNumber, String telcoProvider);
}
