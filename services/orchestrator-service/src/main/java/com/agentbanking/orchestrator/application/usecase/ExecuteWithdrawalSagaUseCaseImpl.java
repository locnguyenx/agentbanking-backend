package com.agentbanking.orchestrator.application.usecase;

import com.agentbanking.orchestrator.domain.port.in.ExecuteWithdrawalSagaUseCase;
import com.agentbanking.orchestrator.domain.port.in.ExecuteWithdrawalSagaUseCase.SagaResult;
import com.agentbanking.orchestrator.domain.port.in.ExecuteWithdrawalSagaUseCase.WithdrawalSagaCommand;
import com.agentbanking.orchestrator.domain.service.TransactionOrchestrator;
import org.springframework.stereotype.Service;

@Service
public class ExecuteWithdrawalSagaUseCaseImpl implements ExecuteWithdrawalSagaUseCase {

    private final TransactionOrchestrator transactionOrchestrator;

    public ExecuteWithdrawalSagaUseCaseImpl(TransactionOrchestrator transactionOrchestrator) {
        this.transactionOrchestrator = transactionOrchestrator;
    }

    @Override
    public SagaResult executeSaga(WithdrawalSagaCommand command) {
        return transactionOrchestrator.executeSaga(command);
    }
}
