package com.agentbanking.orchestrator.infrastructure.temporal;

import com.agentbanking.orchestrator.application.activity.PersistWorkflowResultActivity;
import io.temporal.failure.CanceledFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.function.Supplier;

public class WorkflowExecutor {

    private static final Logger log = Workflow.getLogger(WorkflowExecutor.class);

    public static <T> T execute(
            String workflowType,
            PersistWorkflowResultActivity persistActivity,
            Supplier<T> logic) {
        
        try {
            return logic.get();
        } catch (CanceledFailure e) {
            log.error("Workflow {} timed out: {}", workflowType, e.getMessage());
            
            String workflowId = Workflow.getInfo().getWorkflowId();
            String errorCode = "ERR_WORKFLOW_TIMEOUT";
            String errorMessage = "Workflow timed out - maximum execution time exceeded";
            
            try {
                persistActivity.persistResult(new PersistWorkflowResultActivity.Input(
                        workflowId,
                        "FAILED",
                        errorCode,
                        errorMessage,
                        null,
                        null,
                        null,
                        "Workflow timeout: " + workflowType
                ));
                log.info("Timeout status persisted for workflow: {}", workflowId);
            } catch (Exception persistError) {
                log.error("Failed to persist timeout status for {}: {}", workflowId, persistError.getMessage());
            }
            
            throw e;
        }
    }
}