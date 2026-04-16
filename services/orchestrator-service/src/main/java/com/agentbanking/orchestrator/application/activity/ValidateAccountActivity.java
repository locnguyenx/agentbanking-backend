package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.AccountValidationInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.AccountValidationResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ValidateAccountActivity {
    @ActivityMethod(name = "ValidateAccount")
    AccountValidationResult validateAccount(AccountValidationInput input);
}
