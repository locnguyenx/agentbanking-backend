package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.PersistWorkflowResultActivity;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class PersistWorkflowResultActivityImpl implements PersistWorkflowResultActivity {

    private static final Logger log = LoggerFactory.getLogger(PersistWorkflowResultActivityImpl.class);

    private final TransactionRecordRepository transactionRecordRepository;

    public PersistWorkflowResultActivityImpl(TransactionRecordRepository transactionRecordRepository) {
        this.transactionRecordRepository = transactionRecordRepository;
    }

    @Override
    public void persistResult(Input input) {
        log.info("Persisting workflow result: workflowId={}, status={}, referenceNumber={}, pendingReason={}",
                input.workflowId(), input.status(), input.referenceNumber(), input.pendingReason());
        transactionRecordRepository.updateStatus(
                input.workflowId(),
                input.status(),
                input.errorCode(),
                input.errorMessage(),
                input.externalReference(),
                input.customerFee(),
                input.referenceNumber(),
                input.pendingReason(),
                input.errorMessage()
        );
    }
}
