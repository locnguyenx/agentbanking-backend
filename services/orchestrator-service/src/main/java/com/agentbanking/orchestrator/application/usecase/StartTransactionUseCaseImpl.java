package com.agentbanking.orchestrator.application.usecase;

import com.agentbanking.orchestrator.application.workflow.*;
import com.agentbanking.orchestrator.domain.model.TransactionType;
import com.agentbanking.orchestrator.domain.port.in.StartTransactionUseCase;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
import com.agentbanking.orchestrator.domain.service.WorkflowRouter;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StartTransactionUseCaseImpl implements StartTransactionUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartTransactionUseCaseImpl.class);

    private final WorkflowFactory workflowFactory;
    private final WorkflowRouter workflowRouter;
    private final TransactionRecordRepository transactionRecordRepository;

    public StartTransactionUseCaseImpl(WorkflowFactory workflowFactory,
                                        WorkflowRouter workflowRouter,
                                        TransactionRecordRepository transactionRecordRepository) {
        this.workflowFactory = workflowFactory;
        this.workflowRouter = workflowRouter;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @Override
    public StartTransactionResult start(StartTransactionCommand command) {
        String workflowType = workflowRouter.determineWorkflowType(
                command.transactionType(), command.targetBIN());
        
        String workflowId = workflowFactory.startWorkflow(
                command.idempotencyKey(),
                command.transactionType().name(),
                buildWorkflowInput(command));
        
        transactionRecordRepository.create(
                UUID.randomUUID(),
                workflowId,
                command.transactionType(),
                command.agentId(),
                command.amount(),
                "PENDING");
        
        log.info("Started {} workflow with id: {}", workflowType, workflowId);
        
        return new StartTransactionResult("PENDING", workflowId, 
                "/api/v1/transactions/" + workflowId + "/status");
    }

    private Object buildWorkflowInput(StartTransactionCommand command) {
        return switch (command.transactionType()) {
            case CASH_WITHDRAWAL -> new WithdrawalWorkflow.WithdrawalInput(
                    command.agentId(),
                    command.pan(),
                    command.pinBlock(),
                    command.amount(),
                    command.idempotencyKey(),
                    command.customerCardMasked(),
                    command.geofenceLat(),
                    command.geofenceLng(),
                    command.customerMykad(),
                    command.agentTier());
            case CASH_DEPOSIT -> new DepositWorkflow.DepositInput(
                    command.agentId(),
                    command.destinationAccount(),
                    command.amount(),
                    command.idempotencyKey(),
                    command.customerMykad(),
                    command.geofenceLat(),
                    command.geofenceLng(),
                    command.requiresBiometric(),
                    command.agentTier());
            case BILL_PAYMENT -> new BillPaymentWorkflow.BillPaymentInput(
                    command.agentId(),
                    command.billerCode(),
                    command.ref1(),
                    command.ref2(),
                    command.amount(),
                    command.idempotencyKey(),
                    command.customerMykad(),
                    command.geofenceLat(),
                    command.geofenceLng(),
                    command.agentTier());
            case DUITNOW_TRANSFER -> new DuitNowTransferWorkflow.DuitNowTransferInput(
                    command.agentId(),
                    command.proxyType(),
                    command.proxyValue(),
                    command.amount(),
                    command.idempotencyKey(),
                    command.customerMykad(),
                    command.geofenceLat(),
                    command.geofenceLng(),
                    command.agentTier());
        };
    }
}
