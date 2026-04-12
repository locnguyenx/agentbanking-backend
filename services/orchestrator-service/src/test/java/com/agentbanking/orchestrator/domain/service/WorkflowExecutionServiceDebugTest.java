package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.infrastructure.web.dto.WorkflowExecutionDetailsResponse;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.history.v1.*;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryRequest;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryResponse;
import io.temporal.api.workflowservice.v1.WorkflowServiceGrpc;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.client.WorkflowStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Simple test to debug external service status functionality.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowExecutionServiceDebugTest {

    @Mock
    private WorkflowFactory workflowFactory;

    @Mock
    private WorkflowClient workflowClient;

    @Mock
    private WorkflowServiceStubs workflowServiceStubs;

    @Mock
    private WorkflowServiceGrpc.WorkflowServiceBlockingStub workflowServiceBlockingStub;

    private WorkflowExecutionService workflowExecutionService;

    @BeforeEach
    void setUp() {
        when(workflowClient.getWorkflowServiceStubs()).thenReturn(workflowServiceStubs);
        when(workflowServiceStubs.blockingStub()).thenReturn(workflowServiceBlockingStub);
        workflowExecutionService = new WorkflowExecutionService(workflowFactory, workflowClient, "test-namespace");
    }

    @Test
    void debugExternalServiceStatus() {
        // Given
        String workflowId = "WF-DEBUG-001";
        History mockHistory = createSimpleHistoryWithTimeout();
        GetWorkflowExecutionHistoryResponse mockResponse = GetWorkflowExecutionHistoryResponse.newBuilder()
                .setHistory(mockHistory)
                .build();
        
        when(workflowServiceBlockingStub.getWorkflowExecutionHistory(any(GetWorkflowExecutionHistoryRequest.class)))
                .thenReturn(mockResponse);

        // When
        WorkflowExecutionDetailsResponse result = workflowExecutionService.getExecutionDetails(workflowId);

        // Then - debug the actual values
        System.out.println("External Service Status:");
        System.out.println("  Rules Service: " + result.externalServiceStatus().rulesService());
        System.out.println("  Ledger Service: " + result.externalServiceStatus().ledgerService());
        System.out.println("  Switch Adapter: " + result.externalServiceStatus().switchAdapter());
        System.out.println("  Biller Service: " + result.externalServiceStatus().billerService());
        
        // Let's see what we actually get
        assertThat(result.externalServiceStatus()).isNotNull();
        
        // The timed-out activity should be detected as switch service
        assertThat(result.externalServiceStatus().switchAdapter()).isEqualTo(WorkflowExecutionDetailsResponse.ServiceStatus.TIMEOUT);
    }

    private History createSimpleHistoryWithTimeout() {
        Instant baseTime = Instant.parse("2026-04-11T10:30:00Z");
        
        return History.newBuilder()
                .addEvents(createScheduledEvent(1, "CheckVelocityActivity", baseTime.plusSeconds(1))) // Rules service - completed
                .addEvents(createStartedEvent(2, 1, baseTime.plusSeconds(2)))
                .addEvents(createCompletedEvent(3, 1, baseTime.plusSeconds(3)))
                .addEvents(createScheduledEvent(4, "AuthorizeAtSwitchActivity", baseTime.plusSeconds(4))) // Switch service - timed out
                .addEvents(createStartedEvent(5, 4, baseTime.plusSeconds(5)))
                .addEvents(createTimedOutEvent(6, 4, baseTime.plusSeconds(30)))
                .build();
    }

    // Helper methods (same as in the main test)
    private HistoryEvent createScheduledEvent(long eventId, String activityType, Instant eventTime) {
        return HistoryEvent.newBuilder()
                .setEventId(eventId)
                .setEventType(EventType.EVENT_TYPE_ACTIVITY_TASK_SCHEDULED)
                .setEventTime(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(eventTime.getEpochSecond())
                        .setNanos(eventTime.getNano())
                        .build())
                .setActivityTaskScheduledEventAttributes(
                        ActivityTaskScheduledEventAttributes.newBuilder()
                                .setActivityType(io.temporal.api.common.v1.ActivityType.newBuilder()
                                        .setName(activityType)
                                        .build())
                                .build())
                .build();
    }

    private HistoryEvent createStartedEvent(long eventId, long scheduledEventId, Instant eventTime) {
        return HistoryEvent.newBuilder()
                .setEventId(eventId)
                .setEventType(EventType.EVENT_TYPE_ACTIVITY_TASK_STARTED)
                .setEventTime(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(eventTime.getEpochSecond())
                        .setNanos(eventTime.getNano())
                        .build())
                .setActivityTaskStartedEventAttributes(
                        ActivityTaskStartedEventAttributes.newBuilder()
                                .setScheduledEventId(scheduledEventId)
                                .setAttempt(1)
                                .build())
                .build();
    }

    private HistoryEvent createCompletedEvent(long eventId, long scheduledEventId, Instant eventTime) {
        return HistoryEvent.newBuilder()
                .setEventId(eventId)
                .setEventType(EventType.EVENT_TYPE_ACTIVITY_TASK_COMPLETED)
                .setEventTime(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(eventTime.getEpochSecond())
                        .setNanos(eventTime.getNano())
                        .build())
                .setActivityTaskCompletedEventAttributes(
                        ActivityTaskCompletedEventAttributes.newBuilder()
                                .setScheduledEventId(scheduledEventId)
                                .build())
                .build();
    }

    private HistoryEvent createTimedOutEvent(long eventId, long scheduledEventId, Instant eventTime) {
        return HistoryEvent.newBuilder()
                .setEventId(eventId)
                .setEventType(EventType.EVENT_TYPE_ACTIVITY_TASK_TIMED_OUT)
                .setEventTime(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(eventTime.getEpochSecond())
                        .setNanos(eventTime.getNano())
                        .build())
                .setActivityTaskTimedOutEventAttributes(
                        ActivityTaskTimedOutEventAttributes.newBuilder()
                                .setScheduledEventId(scheduledEventId)
                                .build())
                .build();
    }
}