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
 * Unit tests for WorkflowExecutionService.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowExecutionServiceTest {

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
    void shouldReturnExecutionDetailsForRunningWorkflow() {
        // Given
        String workflowId = "WF-TEST-123";
        History mockHistory = createMockHistoryWithRunningActivity();
        GetWorkflowExecutionHistoryResponse mockResponse = GetWorkflowExecutionHistoryResponse.newBuilder()
                .setHistory(mockHistory)
                .build();
        
        when(workflowServiceBlockingStub.getWorkflowExecutionHistory(any(GetWorkflowExecutionHistoryRequest.class)))
                .thenReturn(mockResponse);

        // When
        WorkflowExecutionDetailsResponse result = workflowExecutionService.getExecutionDetails(workflowId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.workflowId()).isEqualTo(workflowId);
        assertThat(result.currentStatus()).isEqualTo("RUNNING");
        assertThat(result.currentActivity()).isNotNull();
        assertThat(result.currentActivity().name()).isEqualTo("EvaluateSTPActivity");
        assertThat(result.currentActivity().status()).isEqualTo(WorkflowExecutionDetailsResponse.ActivityStatus.RUNNING);
        assertThat(result.activityTimeline()).hasSize(2);
        
        var firstActivity = result.activityTimeline().get(0);
        assertThat(firstActivity.name()).isEqualTo("CheckVelocityActivity");
        assertThat(firstActivity.status()).isEqualTo(WorkflowExecutionDetailsResponse.ActivityStatus.COMPLETED);
    }

    @Test
    void shouldReturnExecutionDetailsForCompletedWorkflow() {
        // Given
        String workflowId = "WF-COMPLETED-456";
        History mockHistory = createMockHistoryWithCompletedActivities();
        GetWorkflowExecutionHistoryResponse mockResponse = GetWorkflowExecutionHistoryResponse.newBuilder()
                .setHistory(mockHistory)
                .build();
        
        when(workflowServiceBlockingStub.getWorkflowExecutionHistory(any(GetWorkflowExecutionHistoryRequest.class)))
                .thenReturn(mockResponse);

        // When
        WorkflowExecutionDetailsResponse result = workflowExecutionService.getExecutionDetails(workflowId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.workflowId()).isEqualTo(workflowId);
        assertThat(result.currentStatus()).isEqualTo("COMPLETED");
        assertThat(result.currentActivity()).isNull(); // No current activity when completed
        assertThat(result.activityTimeline()).hasSize(3);
        
        // All activities should be completed
        result.activityTimeline().forEach(activity -> {
            assertThat(activity.status()).isEqualTo(WorkflowExecutionDetailsResponse.ActivityStatus.COMPLETED);
        });
    }

    @Test
    void shouldReturnExecutionDetailsForFailedWorkflow() {
        // Given
        String workflowId = "WF-FAILED-789";
        History mockHistory = createMockHistoryWithFailedActivity();
        GetWorkflowExecutionHistoryResponse mockResponse = GetWorkflowExecutionHistoryResponse.newBuilder()
                .setHistory(mockHistory)
                .build();
        
        when(workflowServiceBlockingStub.getWorkflowExecutionHistory(any(GetWorkflowExecutionHistoryRequest.class)))
                .thenReturn(mockResponse);

        // When
        WorkflowExecutionDetailsResponse result = workflowExecutionService.getExecutionDetails(workflowId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.workflowId()).isEqualTo(workflowId);
        assertThat(result.currentStatus()).isEqualTo("FAILED");
        assertThat(result.activityTimeline()).hasSize(2);
        
        var failedActivity = result.activityTimeline().stream()
                .filter(a -> a.name().equals("AuthorizeAtSwitchActivity"))
                .findFirst()
                .orElseThrow();
        
        assertThat(failedActivity.status()).isEqualTo(WorkflowExecutionDetailsResponse.ActivityStatus.FAILED);
        assertThat(failedActivity.pendingReason()).contains("Switch authorization failed");
    }

    @Test
    void shouldHandleTemporalExceptionGracefully() {
        // Given
        String workflowId = "WF-ERROR-001";
        
        when(workflowServiceBlockingStub.getWorkflowExecutionHistory(any(GetWorkflowExecutionHistoryRequest.class)))
                .thenThrow(new RuntimeException("Temporal connection failed"));

        // When
        WorkflowExecutionDetailsResponse result = workflowExecutionService.getExecutionDetails(workflowId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.workflowId()).isEqualTo(workflowId);
        assertThat(result.currentStatus()).isEqualTo("UNKNOWN");
        assertThat(result.activityTimeline()).isEmpty();
        assertThat(result.debugInfo()).containsEntry("error", "Failed to retrieve execution details: Temporal connection failed");
        assertThat(result.debugInfo()).containsEntry("fallback", true);
    }

    @Test
    void shouldHandleEmptyHistoryGracefully() {
        // Given
        String workflowId = "WF-EMPTY-001";
        History emptyHistory = History.newBuilder().build();
        GetWorkflowExecutionHistoryResponse mockResponse = GetWorkflowExecutionHistoryResponse.newBuilder()
                .setHistory(emptyHistory)
                .build();
        
        when(workflowServiceBlockingStub.getWorkflowExecutionHistory(any(GetWorkflowExecutionHistoryRequest.class)))
                .thenReturn(mockResponse);

        // When
        WorkflowExecutionDetailsResponse result = workflowExecutionService.getExecutionDetails(workflowId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.workflowId()).isEqualTo(workflowId);
        assertThat(result.currentStatus()).isEqualTo("UNKNOWN");
        assertThat(result.activityTimeline()).isEmpty();
        assertThat(result.debugInfo()).containsEntry("error", "No execution history available");
        assertThat(result.debugInfo()).containsEntry("fallback", true);
    }

    @Test
    void shouldDetermineExternalServiceStatusCorrectly() {
        // Given
        String workflowId = "WF-SERVICES-001";
        History mockHistory = createMockHistoryWithServiceActivities();
        GetWorkflowExecutionHistoryResponse mockResponse = GetWorkflowExecutionHistoryResponse.newBuilder()
                .setHistory(mockHistory)
                .build();
        
        when(workflowServiceBlockingStub.getWorkflowExecutionHistory(any(GetWorkflowExecutionHistoryRequest.class)))
                .thenReturn(mockResponse);

        // When
        WorkflowExecutionDetailsResponse result = workflowExecutionService.getExecutionDetails(workflowId);

        // Then
        assertThat(result.externalServiceStatus()).isNotNull();
        assertThat(result.externalServiceStatus().rulesService()).isEqualTo(WorkflowExecutionDetailsResponse.ServiceStatus.RESPONDING);
        assertThat(result.externalServiceStatus().ledgerService()).isEqualTo(WorkflowExecutionDetailsResponse.ServiceStatus.RESPONDING);
        assertThat(result.externalServiceStatus().switchAdapter()).isEqualTo(WorkflowExecutionDetailsResponse.ServiceStatus.TIMEOUT);
    }

    // Helper methods to create mock history data

    private History createMockHistoryWithRunningActivity() {
        Instant baseTime = Instant.parse("2026-04-11T10:30:00Z");
        
        return History.newBuilder()
                .addEvents(createScheduledEvent(1, "CheckVelocityActivity", baseTime.plusSeconds(1)))
                .addEvents(createStartedEvent(2, 1, baseTime.plusSeconds(2)))
                .addEvents(createCompletedEvent(3, 1, baseTime.plusSeconds(3)))
                .addEvents(createScheduledEvent(4, "EvaluateSTPActivity", baseTime.plusSeconds(4)))
                .addEvents(createStartedEvent(5, 4, baseTime.plusSeconds(5)))
                .build();
    }

    private History createMockHistoryWithCompletedActivities() {
        Instant baseTime = Instant.parse("2026-04-11T10:30:00Z");
        
        return History.newBuilder()
                .addEvents(createScheduledEvent(1, "CheckVelocityActivity", baseTime.plusSeconds(1)))
                .addEvents(createStartedEvent(2, 1, baseTime.plusSeconds(2)))
                .addEvents(createCompletedEvent(3, 1, baseTime.plusSeconds(3)))
                .addEvents(createScheduledEvent(4, "EvaluateSTPActivity", baseTime.plusSeconds(4)))
                .addEvents(createStartedEvent(5, 4, baseTime.plusSeconds(5)))
                .addEvents(createCompletedEvent(6, 4, baseTime.plusSeconds(6)))
                .addEvents(createScheduledEvent(7, "BlockFloatActivity", baseTime.plusSeconds(7)))
                .addEvents(createStartedEvent(8, 7, baseTime.plusSeconds(8)))
                .addEvents(createCompletedEvent(9, 7, baseTime.plusSeconds(9)))
                .build();
    }

    private History createMockHistoryWithFailedActivity() {
        Instant baseTime = Instant.parse("2026-04-11T10:30:00Z");
        
        return History.newBuilder()
                .addEvents(createScheduledEvent(1, "CheckVelocityActivity", baseTime.plusSeconds(1)))
                .addEvents(createStartedEvent(2, 1, baseTime.plusSeconds(2)))
                .addEvents(createCompletedEvent(3, 1, baseTime.plusSeconds(3)))
                .addEvents(createScheduledEvent(4, "AuthorizeAtSwitchActivity", baseTime.plusSeconds(4)))
                .addEvents(createStartedEvent(5, 4, baseTime.plusSeconds(5)))
                .addEvents(createFailedEvent(6, 4, baseTime.plusSeconds(6), "Switch authorization failed"))
                .build();
    }

    private History createMockHistoryWithServiceActivities() {
        Instant baseTime = Instant.parse("2026-04-11T10:30:00Z");
        
        return History.newBuilder()
                .addEvents(createScheduledEvent(1, "CheckVelocityActivity", baseTime.plusSeconds(1))) // Rules service
                .addEvents(createStartedEvent(2, 1, baseTime.plusSeconds(2)))
                .addEvents(createCompletedEvent(3, 1, baseTime.plusSeconds(3)))
                .addEvents(createScheduledEvent(4, "BlockFloatActivity", baseTime.plusSeconds(4))) // Ledger service
                .addEvents(createStartedEvent(5, 4, baseTime.plusSeconds(5)))
                .addEvents(createCompletedEvent(6, 4, baseTime.plusSeconds(6)))
                .addEvents(createScheduledEvent(7, "AuthorizeAtSwitchActivity", baseTime.plusSeconds(7))) // Switch service
                .addEvents(createStartedEvent(8, 7, baseTime.plusSeconds(8)))
                .addEvents(createTimedOutEvent(9, 7, baseTime.plusSeconds(30))) // Switch service timeout
                .build();
    }

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

    private HistoryEvent createFailedEvent(long eventId, long scheduledEventId, Instant eventTime, String errorMessage) {
        return HistoryEvent.newBuilder()
                .setEventId(eventId)
                .setEventType(EventType.EVENT_TYPE_ACTIVITY_TASK_FAILED)
                .setEventTime(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(eventTime.getEpochSecond())
                        .setNanos(eventTime.getNano())
                        .build())
                .setActivityTaskFailedEventAttributes(
                        ActivityTaskFailedEventAttributes.newBuilder()
                                .setScheduledEventId(scheduledEventId)
                                .setFailure(io.temporal.api.failure.v1.Failure.newBuilder()
                                        .setMessage(errorMessage)
                                        .build())
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