package com.agentbanking.orchestrator.application.usecase;

import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.in.QueryWorkflowStatusUseCase;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class QueryWorkflowStatusUseCaseImpl implements QueryWorkflowStatusUseCase {

    private final TransactionRecordRepository transactionRecordRepository;

    public QueryWorkflowStatusUseCaseImpl(TransactionRecordRepository transactionRecordRepository) {
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @Override
    public Optional<WorkflowStatusResponse> getStatus(String workflowId) {
        return transactionRecordRepository.findByWorkflowId(workflowId)
                .map(record -> {
                    WorkflowStatus status = mapStatus(record.status());
                    WorkflowResult result = new WorkflowResult(
                            record.status(),
                            record.id(),
                            record.errorCode(),
                            record.errorMessage(),
                            null,
                            record.externalReference(),
                            record.amount(),
                            record.customerFee(),
                            java.util.Map.of(),
                            record.completedAt()
                    );
                    return new WorkflowStatusResponse(status, result);
                });
    }

    private WorkflowStatus mapStatus(String status) {
        return switch (status) {
            case "COMPLETED" -> WorkflowStatus.COMPLETED;
            case "FAILED" -> WorkflowStatus.FAILED;
            case "RUNNING" -> WorkflowStatus.RUNNING;
            case "COMPENSATING" -> WorkflowStatus.COMPENSATING;
            default -> WorkflowStatus.PENDING;
        };
    }
}
