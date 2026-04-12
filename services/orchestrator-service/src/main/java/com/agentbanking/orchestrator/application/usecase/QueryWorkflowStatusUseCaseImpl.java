package com.agentbanking.orchestrator.application.usecase;

import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.in.QueryWorkflowStatusUseCase;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class QueryWorkflowStatusUseCaseImpl implements QueryWorkflowStatusUseCase {

    private final TransactionRecordRepository transactionRecordRepository;
    private final LedgerServicePort ledgerService;

    public QueryWorkflowStatusUseCaseImpl(TransactionRecordRepository transactionRecordRepository, LedgerServicePort ledgerService) {
        this.transactionRecordRepository = transactionRecordRepository;
        this.ledgerService = ledgerService;
    }

    @Override
    public Optional<WorkflowStatusResponse> getStatus(String workflowId) {
        return transactionRecordRepository.findByWorkflowId(workflowId)
                .map(record -> {
                    WorkflowStatus status = mapStatus(record.status());
                    
                    Map<String, Object> metadata = new HashMap<>();
                    if (record.id() != null) {
                        try {
                            var details = ledgerService.getTransactionDetails(record.id());
                            if (details != null) {
                                metadata.put("agentTier", details.agentTier());
                                metadata.put("targetBin", details.targetBin());
                                metadata.put("billerCode", details.billerCode());
                                metadata.put("ref1", details.ref1());
                                metadata.put("ref2", details.ref2());
                                metadata.put("destinationAccount", details.destinationAccount());
                                metadata.put("customerCardMasked", details.customerCardMasked());
                                metadata.put("geofenceLat", details.geofenceLat());
                                metadata.put("geofenceLng", details.geofenceLng());
                                metadata.put("bankShare", details.bankShare());
                                metadata.put("agentCommission", details.agentCommission());
                            }
                        } catch (Exception e) {
                            // Non-blocking log if ledger details are unavailable
                        }
                    }

                    WorkflowResult result = new WorkflowResult(
                            record.status(),
                            record.pendingReason(),
                            record.errorDetails(),
                            record.id(),
                            record.errorCode(),
                            record.errorMessage(),
                            null,
                            record.referenceNumber(),
                            record.amount(),
                            record.customerFee(),
                            metadata,
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
