package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.ValidateAccountActivity;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.AccountValidationInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.AccountValidationResult;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class ValidateAccountActivityImpl implements ValidateAccountActivity {

    private final LedgerServicePort ledgerServicePort;

    public ValidateAccountActivityImpl(LedgerServicePort ledgerServicePort) {
        this.ledgerServicePort = ledgerServicePort;
    }

    @Override
    public AccountValidationResult validateAccount(AccountValidationInput input) {
        return ledgerServicePort.validateAccount(input);
    }
}
