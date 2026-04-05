package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.AccountValidationInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.AccountValidationResult;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface ValidateAccountActivity {
    AccountValidationResult validateAccount(AccountValidationInput input);
}
