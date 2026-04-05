package com.agentbanking.orchestrator.application.usecase;

import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.in.QueryWorkflowStatusUseCase;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import io.temporal.client.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class QueryWorkflowStatusUseCaseImpl implements QueryWorkflowStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(QueryWorkflowStatusUseCaseImpl.class);

    private final WorkflowFactory workflowFactory;
    private final TransactionRecordRepository transactionRecordRepository;

    public QueryWorkflowStatusUseCaseImpl(WorkflowFactory workflowFactory,
                                           TransactionRecordRepository transactionRecordRepository) {
        this.workflowFactory = workflowFactory;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @Override
    public Optional<WorkflowStatusResponse> getStatus(String workflowId) {
        return transactionRecordRepository.findByWorkflowId(workflowId)
                .map(record -> {
                    WorkflowStub workflowStub = workflowFactory.getWorkflowStub(workflowId);
                    
                    WorkflowStatus temporalStatus = workflowStub.getStatus() != null 
                            ? mapTemporalStatus(workflowStub.getStatus())
                            : WorkflowStatus.PENDING;
                    
                    WorkflowResult result = null;
                    if (temporalStatus == WorkflowStatus.COMPLETED || temporalStatus == WorkflowStatus.FAILED) {
                        try {
                            result = workflowStub.getResult(WorkflowResult.class);
                        } catch (Exception e) {
                            log.warn("Could not get workflow result for {}: {}", workflowId, e.getMessage());
                        }
                    }
                    
                    return new WorkflowStatusResponse(temporalStatus, result);
                });
    }

    private WorkflowStatus mapTemporalStatus(io.temporal.api.enums.v1.WorkflowExecutionStatus temporalStatus) {
        return switch (temporalStatus) {
            case WORKFLOW_EXECUTION_STATUS_RUNNING -> WorkflowStatus.RUNNING;
            case WORKFLOW_EXECUTION_STATUS_COMPLETED -> WorkflowStatus.COMPLETED;
            case WORKFLOW_EXECUTION_STATUS_FAILED -> WorkflowStatus.FAILED;
            case WORKFLOW_EXECUTION_STATUS_CANCELED, WORKFLOW_EXECUTION_STATUS_TERMINATED -> WorkflowStatus.COMPENSATING;
            default -> WorkflowStatus.PENDING;
        };
    }
}
