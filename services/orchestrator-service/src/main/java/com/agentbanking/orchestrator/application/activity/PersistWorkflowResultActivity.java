package com.agentbanking.orchestrator.application.activity;



import io.temporal.activity.ActivityInterface;
import java.math.BigDecimal;
import java.time.LocalDateTime;



@ActivityInterface
public interface PersistWorkflowResultActivity {

void persistResult(Input input);

record Input(
String workflowId,
String status,
String errorCode,
String errorMessage,
String externalReference,
BigDecimal customerFee,
String referenceNumber,
String pendingReason,
LocalDateTime completedAt
) {
public Input(
String workflowId,
String status,
String errorCode,
String errorMessage,
String externalReference,
BigDecimal customerFee,
String referenceNumber,
String pendingReason
) {
this(workflowId, status, errorCode, errorMessage, externalReference, customerFee, referenceNumber, pendingReason, null);
}
}
}
