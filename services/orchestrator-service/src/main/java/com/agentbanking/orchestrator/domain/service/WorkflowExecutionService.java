package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.infrastructure.web.dto.WorkflowExecutionDetailsResponse;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.enums.v1.TimeoutType;
import io.temporal.api.history.v1.*;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryRequest;
import io.temporal.api.workflowservice.v1.GetWorkflowExecutionHistoryResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for retrieving detailed workflow execution information from Temporal.
 * Provides comprehensive visibility into workflow activity execution timeline,
 * current status, and external service interactions.
 */
public class WorkflowExecutionService {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionService.class);
    
    private final WorkflowFactory workflowFactory;
    private final WorkflowClient workflowClient;
    private final String temporalNamespace;
    private final TransactionRecordRepository transactionRecordRepository;
    
    public WorkflowExecutionService(
            WorkflowFactory workflowFactory,
            WorkflowClient workflowClient,
            String temporalNamespace,
            TransactionRecordRepository transactionRecordRepository) {
        this.workflowFactory = workflowFactory;
        this.workflowClient = workflowClient;
        this.temporalNamespace = temporalNamespace;
        this.transactionRecordRepository = transactionRecordRepository;
    }
    
    /**
     * Retrieves comprehensive execution details for a specific workflow.
     * 
     * @param workflowId The workflow ID to retrieve details for
     * @return WorkflowExecutionDetailsResponse containing execution timeline and status
     */
    public WorkflowExecutionDetailsResponse getExecutionDetails(String workflowId) {
        try {
            log.info("Retrieving execution details for workflow: {}", workflowId);
            
            // Get the transaction record status from database (source of truth)
            String dbStatus = getTransactionRecordStatus(workflowId);
            log.info("Database status for workflow {}: {}", workflowId, dbStatus);
            
            // Get workflow execution history from Temporal
            History history = getWorkflowHistory(workflowId);
            
            if (history == null || history.getEventsCount() == 0) {
                log.warn("No execution history found for workflow: {}", workflowId);
                return buildFallbackResponse(workflowId, "No execution history available", dbStatus);
            }
            
            // Parse the history to build execution details
            return parseExecutionHistory(history, workflowId, dbStatus);
            
        } catch (Exception e) {
            log.error("Failed to retrieve execution details for workflow {}: {}", workflowId, e.getMessage(), e);
            return buildFallbackResponse(workflowId, "Failed to retrieve execution details: " + e.getMessage(), null);
        }
    }
    
    private String getTransactionRecordStatus(String workflowId) {
        try {
            var record = transactionRecordRepository.findByWorkflowId(workflowId);
            return record.map(r -> r.status()).orElse(null);
        } catch (Exception e) {
            log.warn("Could not get transaction record status for {}: {}", workflowId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Retrieves workflow execution history from Temporal.
     */
    private History getWorkflowHistory(String workflowId) throws Exception {
        GetWorkflowExecutionHistoryRequest request = GetWorkflowExecutionHistoryRequest.newBuilder()
                .setNamespace(temporalNamespace)
                .setExecution(WorkflowExecution.newBuilder()
                        .setWorkflowId(workflowId)
                        .build())
                .setMaximumPageSize(1000)
                .build();
                
        GetWorkflowExecutionHistoryResponse response = workflowClient.getWorkflowServiceStubs().blockingStub().getWorkflowExecutionHistory(request);
        return response.getHistory();
    }
    
    /**
     * Parses Temporal history events to build execution details.
     */
    private WorkflowExecutionDetailsResponse parseExecutionHistory(History history, String workflowId, String dbStatus) {
        List<WorkflowExecutionDetailsResponse.ActivityTimelineItem> timeline = new ArrayList<>();
        WorkflowExecutionDetailsResponse.ActivityDetails currentActivity = null;
        
        // Track activity events by scheduled event ID
        Map<Long, ActivityEventTracker> activityTrackers = new HashMap<>();
        
        for (HistoryEvent event : history.getEventsList()) {
            processHistoryEvent(event, activityTrackers);
        }
        
        // Build timeline from activity trackers
        int sequence = 1;
        for (ActivityEventTracker tracker : activityTrackers.values()) {
            if (tracker.isStarted() || tracker.isCompleted() || tracker.isFailed()) {
                var timelineItem = buildTimelineItem(tracker, sequence++);
                timeline.add(timelineItem);
                
                // Check if this is the current activity
                if (tracker.isRunning()) {
                    currentActivity = buildCurrentActivityDetails(tracker);
                }
            }
        }
        
        // Sort timeline by sequence/start time
        timeline.sort(Comparator.comparing(WorkflowExecutionDetailsResponse.ActivityTimelineItem::sequence));
        
        // Determine current status - use database status as source of truth
        String currentStatus = dbStatus != null ? dbStatus : determineCurrentStatus(timeline, currentActivity);
        
        // Build external service status
        var externalServiceStatus = buildExternalServiceStatus(timeline);
        
        // Estimate completion time
        var estimatedCompletion = estimateCompletion(timeline, currentActivity);
        
        // Build debug info
        var debugInfo = buildDebugInfo(workflowId, history);
        
        return new WorkflowExecutionDetailsResponse(
                workflowId,
                currentStatus,
                currentActivity,
                timeline,
                externalServiceStatus,
                estimatedCompletion,
                debugInfo
        );
    }
    
    /**
     * Processes individual history events to track activity execution.
     */
    private void processHistoryEvent(HistoryEvent event, Map<Long, ActivityEventTracker> activityTrackers) {
        switch (event.getEventType()) {
            case EVENT_TYPE_ACTIVITY_TASK_SCHEDULED:
                handleActivityScheduled(event, activityTrackers);
                break;
            case EVENT_TYPE_ACTIVITY_TASK_STARTED:
                handleActivityStarted(event, activityTrackers);
                break;
            case EVENT_TYPE_ACTIVITY_TASK_COMPLETED:
                handleActivityCompleted(event, activityTrackers);
                break;
            case EVENT_TYPE_ACTIVITY_TASK_FAILED:
                handleActivityFailed(event, activityTrackers);
                break;
            case EVENT_TYPE_ACTIVITY_TASK_TIMED_OUT:
                handleActivityTimedOut(event, activityTrackers);
                break;
            default:
                // Ignore other event types for now
                break;
        }
    }
    
    private void handleActivityScheduled(HistoryEvent event, Map<Long, ActivityEventTracker> trackers) {
        ActivityTaskScheduledEventAttributes attrs = event.getActivityTaskScheduledEventAttributes();
        long scheduledEventId = event.getEventId();
        
        ActivityEventTracker tracker = new ActivityEventTracker(
                scheduledEventId,
                attrs.getActivityType().getName(),
                Instant.ofEpochMilli(event.getEventTime().getSeconds() * 1000)
        );
        
        // Extract input parameters
        if (attrs.hasInput()) {
            tracker.setInput(parsePayloads(attrs.getInput()));
        }
        
        trackers.put(scheduledEventId, tracker);
    }
    
    private void handleActivityStarted(HistoryEvent event, Map<Long, ActivityEventTracker> trackers) {
        ActivityTaskStartedEventAttributes attrs = event.getActivityTaskStartedEventAttributes();
        long scheduledEventId = attrs.getScheduledEventId();
        
        ActivityEventTracker tracker = trackers.get(scheduledEventId);
        if (tracker != null) {
            tracker.markStarted(Instant.ofEpochMilli(event.getEventTime().getSeconds() * 1000));
            tracker.setRetryAttempt(attrs.getAttempt());
        }
    }
    
    private void handleActivityCompleted(HistoryEvent event, Map<Long, ActivityEventTracker> trackers) {
        ActivityTaskCompletedEventAttributes attrs = event.getActivityTaskCompletedEventAttributes();
        long scheduledEventId = attrs.getScheduledEventId();
        
        ActivityEventTracker tracker = trackers.get(scheduledEventId);
        if (tracker != null) {
            tracker.markCompleted(Instant.ofEpochMilli(event.getEventTime().getSeconds() * 1000));
            
            // Extract output result
            if (attrs.hasResult()) {
                tracker.setOutput(parsePayloads(attrs.getResult()));
            }
        }
    }
    
    private void handleActivityFailed(HistoryEvent event, Map<Long, ActivityEventTracker> trackers) {
        ActivityTaskFailedEventAttributes attrs = event.getActivityTaskFailedEventAttributes();
        long scheduledEventId = attrs.getScheduledEventId();
        
        ActivityEventTracker tracker = trackers.get(scheduledEventId);
        if (tracker != null) {
            tracker.markFailed(Instant.ofEpochMilli(event.getEventTime().getSeconds() * 1000));
            tracker.setErrorMessage(attrs.getFailure().getMessage());
        }
    }
    
    private void handleActivityTimedOut(HistoryEvent event, Map<Long, ActivityEventTracker> trackers) {
        ActivityTaskTimedOutEventAttributes attrs = event.getActivityTaskTimedOutEventAttributes();
        long scheduledEventId = attrs.getScheduledEventId();
        
        ActivityEventTracker tracker = trackers.get(scheduledEventId);
        if (tracker != null) {
            tracker.markTimedOut(Instant.ofEpochMilli(event.getEventTime().getSeconds() * 1000));
            String timeoutType = "TIMEOUT"; // Simplified - in real implementation would get from attrs
            tracker.setErrorMessage("Activity timed out: " + timeoutType);
        }
    }
    
    /**
     * Builds a timeline item from an activity tracker.
     */
    private WorkflowExecutionDetailsResponse.ActivityTimelineItem buildTimelineItem(
            ActivityEventTracker tracker, int sequence) {
        
        String duration = calculateDuration(tracker.getStartTime(), tracker.getEndTime());
        String status = determineActivityStatus(tracker);
        
        return new WorkflowExecutionDetailsResponse.ActivityTimelineItem(
                sequence,
                tracker.getActivityName(),
                status,
                formatInstant(tracker.getScheduledTime()),
                duration,
                tracker.getInput(),
                tracker.getOutput(),
                tracker.getErrorMessage()
        );
    }
    
    /**
     * Builds current activity details from an activity tracker.
     */
    private WorkflowExecutionDetailsResponse.ActivityDetails buildCurrentActivityDetails(
            ActivityEventTracker tracker) {
        
        String elapsedTime = calculateElapsedTime(tracker.getStartTime());
        String status = tracker.isRunning() ? WorkflowExecutionDetailsResponse.ActivityStatus.RUNNING : 
                       tracker.isCompleted() ? WorkflowExecutionDetailsResponse.ActivityStatus.COMPLETED :
                       tracker.isFailed() ? WorkflowExecutionDetailsResponse.ActivityStatus.FAILED :
                       WorkflowExecutionDetailsResponse.ActivityStatus.SCHEDULED;
        
        return new WorkflowExecutionDetailsResponse.ActivityDetails(
                tracker.getActivityName(),
                status,
                formatInstant(tracker.getStartTime()),
                elapsedTime,
                tracker.getRetryAttempt(),
                3, // Default max retries - could be configurable
                tracker.getInput(),
                tracker.getOutput(),
                tracker.getErrorMessage()
        );
    }
    
    /**
     * Determines the overall workflow status based on timeline and current activity.
     */
    private String determineCurrentStatus(
            List<WorkflowExecutionDetailsResponse.ActivityTimelineItem> timeline,
            WorkflowExecutionDetailsResponse.ActivityDetails currentActivity) {
        
        if (currentActivity != null && WorkflowExecutionDetailsResponse.ActivityStatus.RUNNING.equals(currentActivity.status())) {
            return "RUNNING";
        }
        
        if (currentActivity != null && WorkflowExecutionDetailsResponse.ActivityStatus.FAILED.equals(currentActivity.status())) {
            return "FAILED";
        }
        
        // Check if any activity in timeline failed
        boolean hasFailedActivity = timeline.stream()
                .anyMatch(item -> WorkflowExecutionDetailsResponse.ActivityStatus.FAILED.equals(item.status()));
        
        if (hasFailedActivity) {
            return "FAILED";
        }
        
        // Check if all activities completed
        boolean allCompleted = timeline.stream()
                .allMatch(item -> WorkflowExecutionDetailsResponse.ActivityStatus.COMPLETED.equals(item.status()));
        
        if (allCompleted) {
            return "COMPLETED";
        }
        
        return "PENDING";
    }
    
    /**
     * Builds external service status based on activity execution.
     */
    private WorkflowExecutionDetailsResponse.ExternalServiceStatus buildExternalServiceStatus(
            List<WorkflowExecutionDetailsResponse.ActivityTimelineItem> timeline) {
        
        // Analyze timeline to determine service health
        Map<String, String> serviceStatusMap = new HashMap<>();
        
        for (var item : timeline) {
            String activityName = item.name().toLowerCase();
            String newStatus = determineServiceHealth(item);
            
            log.debug("Analyzing activity: {} -> status: {}", activityName, newStatus);
            
            String serviceKey = null;
            if (activityName.contains("velocity") || activityName.contains("rules")) {
                serviceKey = "rulesService";
            } else if (activityName.contains("float") || activityName.contains("ledger")) {
                serviceKey = "ledgerService";
            } else if (activityName.contains("switch") || activityName.contains("authorize")) {
                serviceKey = "switchAdapter";
            } else if (activityName.contains("biller")) {
                serviceKey = "billerService";
            }
            
            if (serviceKey != null) {
                String currentStatus = serviceStatusMap.get(serviceKey);
                // Only update if current status is less severe or not set
                if (currentStatus == null || isMoreSevere(newStatus, currentStatus)) {
                    serviceStatusMap.put(serviceKey, newStatus);
                }
            }
        }
        
        log.debug("Final service status map: {}", serviceStatusMap);
        
        return new WorkflowExecutionDetailsResponse.ExternalServiceStatus(
                serviceStatusMap.getOrDefault("rulesService", WorkflowExecutionDetailsResponse.ServiceStatus.NOT_REQUIRED),
                serviceStatusMap.getOrDefault("ledgerService", WorkflowExecutionDetailsResponse.ServiceStatus.NOT_REQUIRED),
                serviceStatusMap.getOrDefault("switchAdapter", WorkflowExecutionDetailsResponse.ServiceStatus.NOT_REQUIRED),
                serviceStatusMap.getOrDefault("billerService", WorkflowExecutionDetailsResponse.ServiceStatus.NOT_REQUIRED)
        );
    }
    
    private boolean isMoreSevere(String newStatus, String currentStatus) {
        // Severity order: FAILED > TIMEOUT > RESPONDING > AVAILABLE
        Map<String, Integer> severityOrder = Map.of(
            WorkflowExecutionDetailsResponse.ServiceStatus.FAILED, 4,
            WorkflowExecutionDetailsResponse.ServiceStatus.TIMEOUT, 3,
            WorkflowExecutionDetailsResponse.ServiceStatus.RESPONDING, 2,
            WorkflowExecutionDetailsResponse.ServiceStatus.AVAILABLE, 1
        );
        
        return severityOrder.getOrDefault(newStatus, 0) > severityOrder.getOrDefault(currentStatus, 0);
    }
    
    private String determineServiceHealth(WorkflowExecutionDetailsResponse.ActivityTimelineItem item) {
        return switch (item.status()) {
            case WorkflowExecutionDetailsResponse.ActivityStatus.COMPLETED -> 
                WorkflowExecutionDetailsResponse.ServiceStatus.RESPONDING;
            case WorkflowExecutionDetailsResponse.ActivityStatus.FAILED -> 
                WorkflowExecutionDetailsResponse.ServiceStatus.FAILED;
            case WorkflowExecutionDetailsResponse.ActivityStatus.TIMED_OUT -> 
                WorkflowExecutionDetailsResponse.ServiceStatus.TIMEOUT;
            default -> WorkflowExecutionDetailsResponse.ServiceStatus.AVAILABLE;
        };
    }
    
    /**
     * Estimates completion time based on historical patterns.
     */
    private String estimateCompletion(
            List<WorkflowExecutionDetailsResponse.ActivityTimelineItem> timeline,
            WorkflowExecutionDetailsResponse.ActivityDetails currentActivity) {
        
        // Simple estimation based on current activity and remaining steps
        if (currentActivity == null) {
            return null;
        }
        
        int completedSteps = (int) timeline.stream()
                .filter(item -> WorkflowExecutionDetailsResponse.ActivityStatus.COMPLETED.equals(item.status()))
                .count();
        
        int totalSteps = completedSteps + 1; // +1 for current activity
        int remainingSteps = Math.max(1, totalSteps - completedSteps);
        
        // Estimate 2 seconds per remaining step (configurable)
        long estimatedSeconds = remainingSteps * 2L;
        
        return Instant.now().plusSeconds(estimatedSeconds).toString();
    }
    
    /**
     * Builds debug information for troubleshooting.
     */
    private Map<String, Object> buildDebugInfo(String workflowId, History history) {
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("temporalWorkflowId", workflowId);
        debugInfo.put("historyEventCount", history.getEventsCount());
        debugInfo.put("namespace", temporalNamespace);
        debugInfo.put("retrievedAt", Instant.now().toString());
        
        // Add last few events for debugging
        List<Map<String, Object>> recentEvents = history.getEventsList().stream()
                .skip(Math.max(0, history.getEventsCount() - 5))
                .map(event -> {
                    Map<String, Object> eventMap = new HashMap<>();
                    eventMap.put("eventType", event.getEventType().name());
                    eventMap.put("eventTime", formatInstant(Instant.ofEpochMilli(event.getEventTime().getSeconds() * 1000)));
                    eventMap.put("eventId", String.valueOf(event.getEventId()));
                    return eventMap;
                })
                .collect(Collectors.toList());
        
        debugInfo.put("recentEvents", recentEvents);
        
        return debugInfo;
    }
    
    /**
     * Builds fallback response when detailed execution data is unavailable.
     */
    private WorkflowExecutionDetailsResponse buildFallbackResponse(String workflowId, String errorMessage, String dbStatus) {
        String status = dbStatus != null ? dbStatus : "UNKNOWN";
        return new WorkflowExecutionDetailsResponse(
                workflowId,
                status,
                null,
                Collections.emptyList(),
                new WorkflowExecutionDetailsResponse.ExternalServiceStatus(
                        WorkflowExecutionDetailsResponse.ServiceStatus.NOT_REQUIRED,
                        WorkflowExecutionDetailsResponse.ServiceStatus.NOT_REQUIRED,
                        WorkflowExecutionDetailsResponse.ServiceStatus.NOT_REQUIRED,
                        WorkflowExecutionDetailsResponse.ServiceStatus.NOT_REQUIRED
                ),
                null,
                Map.of("error", errorMessage, "fallback", true)
        );
    }
    
    /**
     * Helper method to parse Temporal payload data.
     */
    private Map<String, Object> parsePayload(io.temporal.api.common.v1.Payload payload) {
        try {
            // Simple JSON parsing - in production, might need more sophisticated handling
            String data = payload.getData().toStringUtf8();
            // This is a simplified implementation - actual parsing would depend on payload format
            return Map.of("rawData", data);
        } catch (Exception e) {
            log.warn("Failed to parse payload: {}", e.getMessage());
            return Map.of("parseError", e.getMessage());
        }
    }
    
    /**
     * Helper method to parse Temporal payloads (multiple) data.
     */
    private Map<String, Object> parsePayloads(io.temporal.api.common.v1.Payloads payloads) {
        if (payloads == null || payloads.getPayloadsCount() == 0) {
            return Map.of();
        }
        
        try {
            // For simplicity, return the first payload or combine them
            if (payloads.getPayloadsCount() == 1) {
                return parsePayload(payloads.getPayloads(0));
            } else {
                // If multiple payloads, return them as a list
                List<Map<String, Object>> payloadList = new ArrayList<>();
                for (io.temporal.api.common.v1.Payload payload : payloads.getPayloadsList()) {
                    payloadList.add(parsePayload(payload));
                }
                return Map.of("payloads", payloadList);
            }
        } catch (Exception e) {
            log.warn("Failed to parse payloads: {}", e.getMessage());
            return Map.of("parseError", e.getMessage());
        }
    }
    
    /**
     * Calculates duration between two instants.
     */
    private String calculateDuration(Instant start, Instant end) {
        if (start == null || end == null) {
            return "--";
        }
        Duration duration = Duration.between(start, end);
        return formatDuration(duration);
    }
    
    /**
     * Calculates elapsed time from start instant to now.
     */
    private String calculateElapsedTime(Instant start) {
        if (start == null) {
            return "--";
        }
        Duration elapsed = Duration.between(start, Instant.now());
        return formatDuration(elapsed);
    }
    
    /**
     * Formats duration in human-readable format.
     */
    private String formatDuration(Duration duration) {
        if (duration.toSeconds() < 1) {
            return duration.toMillis() + "ms";
        } else if (duration.toMinutes() < 1) {
            return duration.toSeconds() + "s";
        } else {
            return duration.toMinutes() + "m " + (duration.toSeconds() % 60) + "s";
        }
    }
    
    /**
     * Formats instant as ISO string.
     */
    private String formatInstant(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
    
    /**
     * Determines activity status based on tracker state.
     */
    private String determineActivityStatus(ActivityEventTracker tracker) {
        if (tracker.isTimedOut()) {
            return WorkflowExecutionDetailsResponse.ActivityStatus.TIMED_OUT;
        } else if (tracker.isFailed()) {
            return WorkflowExecutionDetailsResponse.ActivityStatus.FAILED;
        } else if (tracker.isCompleted()) {
            return WorkflowExecutionDetailsResponse.ActivityStatus.COMPLETED;
        } else if (tracker.isRunning()) {
            return WorkflowExecutionDetailsResponse.ActivityStatus.RUNNING;
        } else {
            return WorkflowExecutionDetailsResponse.ActivityStatus.SCHEDULED;
        }
    }
    
    /**
     * Helper class to track activity event state during history parsing.
     */
    private static class ActivityEventTracker {
        private final long scheduledEventId;
        private final String activityName;
        private final Instant scheduledTime;
        private Instant startTime;
        private Instant endTime;
        private int retryAttempt = 1;
        private Map<String, Object> input;
        private Map<String, Object> output;
        private String errorMessage;
        private boolean timedOut = false;
        private boolean failed = false;
        private boolean completed = false;
        
        public ActivityEventTracker(long scheduledEventId, String activityName, Instant scheduledTime) {
            this.scheduledEventId = scheduledEventId;
            this.activityName = activityName;
            this.scheduledTime = scheduledTime;
        }
        
        public void markStarted(Instant startTime) {
            this.startTime = startTime;
        }
        
        public void markCompleted(Instant endTime) {
            this.endTime = endTime;
            this.completed = true;
        }
        
        public void markFailed(Instant endTime) {
            this.endTime = endTime;
            this.failed = true;
        }
        
        public void markTimedOut(Instant endTime) {
            this.endTime = endTime;
            this.timedOut = true;
        }
        
        // Getters
        public long getScheduledEventId() { return scheduledEventId; }
        public String getActivityName() { return activityName; }
        public Instant getScheduledTime() { return scheduledTime; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public int getRetryAttempt() { return retryAttempt; }
        public Map<String, Object> getInput() { return input; }
        public Map<String, Object> getOutput() { return output; }
        public String getErrorMessage() { return errorMessage; }
        
        // Setters
        public void setRetryAttempt(int retryAttempt) { this.retryAttempt = retryAttempt; }
        public void setInput(Map<String, Object> input) { this.input = input; }
        public void setOutput(Map<String, Object> output) { this.output = output; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        // State checks
        public boolean isStarted() { return startTime != null; }
        public boolean isRunning() { return startTime != null && endTime == null; }
        public boolean isCompleted() { return completed; }
        public boolean isFailed() { return failed; }
        public boolean isTimedOut() { return timedOut; }
    }
}