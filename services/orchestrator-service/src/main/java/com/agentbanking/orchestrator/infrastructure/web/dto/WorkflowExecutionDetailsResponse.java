package com.agentbanking.orchestrator.infrastructure.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for enhanced workflow execution details.
 * Provides comprehensive information about workflow execution timeline,
 * current activity status, and external service health.
 */
public record WorkflowExecutionDetailsResponse(
    String workflowId,
    String currentStatus,
    ActivityDetails currentActivity,
    List<ActivityTimelineItem> activityTimeline,
    ExternalServiceStatus externalServiceStatus,
    String estimatedCompletion,
    Map<String, Object> debugInfo
) {
    
    /**
     * Details about the currently executing activity.
     */
    public record ActivityDetails(
        String name,
        String status,
        String startTime,
        String elapsedTime,
        int retryAttempt,
        int maxRetries,
        Map<String, Object> input,
        Map<String, Object> output,
        String errorMessage
    ) {}
    
    /**
     * Timeline item representing a single activity execution.
     */
    public record ActivityTimelineItem(
        int sequence,
        String name,
        String status,
        String startTime,
        String duration,
        Map<String, Object> input,
        Map<String, Object> output,
        String pendingReason
    ) {}
    
    /**
     * Status of external services used by the workflow.
     */
    public record ExternalServiceStatus(
        String rulesService,
        String ledgerService,
        String switchAdapter,
        String billerService
    ) {}
    
    /**
     * Activity status constants for consistent status reporting.
     */
    public static class ActivityStatus {
        public static final String SCHEDULED = "SCHEDULED";
        public static final String RUNNING = "RUNNING";
        public static final String COMPLETED = "COMPLETED";
        public static final String FAILED = "FAILED";
        public static final String TIMED_OUT = "TIMED_OUT";
        public static final String CANCELLED = "CANCELLED";
    }
    
    /**
     * External service status constants.
     */
    public static class ServiceStatus {
        public static final String AVAILABLE = "AVAILABLE";
        public static final String RESPONDING = "RESPONDING";
        public static final String TIMEOUT = "TIMEOUT";
        public static final String FAILED = "FAILED";
        public static final String NOT_REQUIRED = "NOT_REQUIRED";
    }
}