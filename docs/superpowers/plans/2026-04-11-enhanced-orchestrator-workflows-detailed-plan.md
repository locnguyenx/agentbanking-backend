# Implementation Plan: Enhanced Orchestrator Workflows Page

## Implementation Status

### ✅ COMPLETED - Backend (Phase 1)
- [x] WorkflowExecutionService created
- [x] Response DTOs created (WorkflowExecutionDetailsResponse, ActivityDetails, ActivityTimelineItem, ExternalServiceStatus)
- [x] ResolutionController endpoint: `/transactions/{workflowId}/execution-details`
- [x] Temporal integration for workflow history retrieval

### ✅ COMPLETED - Frontend Components Created (Phase 2)
- [x] WorkflowCard.tsx - Card view with progress indicators
- [x] ActivityTimelineModal.tsx - Enhanced modal with timeline
- [x] Types in workflow.ts - EnhancedWorkflowItem, ActivityDetails, etc.
- [x] API client method: getWorkflowExecutionDetails

### ❌ NOT COMPLETED - Frontend Integration Required (Phase 2 continued)
- [ ] Add view mode toggle (Table/Card) to OrchestratorWorkflows page
- [ ] Integrate WorkflowCard component into main page
- [ ] Replace WorkflowDetailModal with ActivityTimelineModal
- [ ] Update component props and data mapping

### Phase 3-6: Advanced Features - NOT STARTED
- [ ] External Service Status Panel (advanced)
- [ ] Debug Information Panel (advanced)
- [ ] Performance optimization (virtual scrolling)

---

## Phase 1: Backend Foundation (Week 1-2)

### Week 1: Core Service Implementation
**Day 1-2: Create WorkflowExecutionService**

```java
// New service class in orchestrator-service
package com.agentbanking.orchestrator.domain.service;
import io.temporal.api.history.v1.History;
import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowStub;
import org.springframework.stereotype.Service;
@Service
public class WorkflowExecutionService {
    
    private final WorkflowFactory workflowFactory;
    private final TemporalProperties temporalConfig;
    
    public WorkflowExecutionDetails getExecutionDetails(String workflowId) {
        try {
            WorkflowStub workflowStub = workflowFactory.getWorkflowStub(workflowId);
            
            // Get workflow execution history from Temporal
            GetWorkflowExecutionHistoryRequest historyRequest = 
                GetWorkflowExecutionHistoryRequest.newBuilder()
                    .setNamespace(temporalConfig.getNamespace())
                    .setExecution(WorkflowExecution.newBuilder()
                        .setWorkflowId(workflowId)
                        .build())
                    .setMaximumPageSize(100)
                    .build();
                    
            History history = workflowStub.getExecutionHistory(historyRequest);
            
            return parseExecutionHistory(history, workflowId);
            
        } catch (Exception e) {
            log.error("Failed to get execution details for workflow {}: {}", workflowId, e.getMessage());
            return buildFallbackExecutionDetails(workflowId, e);
        }
    }
    
    private WorkflowExecutionDetails parseExecutionHistory(History history, String workflowId) {
        List<ActivityTimelineItem> timeline = new ArrayList<>();
        ActivityDetails currentActivity = null;
        
        for (HistoryEvent event : history.getEventsList()) {
            switch (event.getEventType()) {
                case EVENT_TYPE_ACTIVITY_TASK_SCHEDULED:
                    timeline.add(buildScheduledActivity(event));
                    break;
                case EVENT_TYPE_ACTIVITY_TASK_STARTED:
                    currentActivity = buildCurrentActivity(event);
                    break;
                case EVENT_TYPE_ACTIVITY_TASK_COMPLETED:
                    timeline.add(buildCompletedActivity(event));
                    break;
                case EVENT_TYPE_ACTIVITY_TASK_FAILED:
                    timeline.add(buildFailedActivity(event));
                    break;
            }
        }
        
        return WorkflowExecutionDetails.builder()
            .workflowId(workflowId)
            .activityTimeline(timeline)
            .currentActivity(currentActivity)
            .externalServiceStatus(getExternalServiceStatus(timeline))
            .build();
    }
}
```
**Day 3-4: Create Response DTOs**

```java
// New DTO in orchestrator-service
public record WorkflowExecutionDetailsResponse(
    String workflowId,
    String currentStatus,
    ActivityDetails currentActivity,
    List<ActivityTimelineItem> activityTimeline,
    ExternalServiceStatus externalServiceStatus,
    String estimatedCompletion,
    Map<String, Object> debugInfo
) {
    
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
    
    public record ExternalServiceStatus(
        String rulesService,
        String ledgerService,
        String switchAdapter,
        String billerService
    ) {}
}
```
**Day 5: Update ResolutionController**
```java
// Add to ResolutionController.java
@GetMapping("/{workflowId}/execution-details")
public ResponseEntity<?> getWorkflowExecutionDetails(@PathVariable String workflowId) {
    try {
        WorkflowExecutionDetails details = workflowExecutionService.getExecutionDetails(workflowId);
        return ResponseEntity.ok(mapToExecutionDetailsResponse(details));
    } catch (Exception e) {
        log.error("Failed to get execution details for workflow {}: {}", workflowId, e.getMessage());
        return ResponseEntity.status(500).body(buildErrorResponse(
            "ERR_SYS_EXECUTION_DETAILS_FAILED", 
            "Failed to retrieve workflow execution details", 
            "RETRY"
        ));
    }
}
```

### Week 2: Integration & Testing

**Day 1-3: Temporal Integration Testing**
- Test with various workflow types (Withdrawal, Deposit, Bill Payment)
- Verify activity timeline parsing accuracy
- Test error handling for unavailable workflows

**Day 4-5: Unit Tests & Documentation**
```java
@Test
public void shouldParseExecutionHistoryForPendingWorkflow() {
    // Given
    String workflowId = "WF-TEST-123";
    History mockHistory = createMockHistoryWithPendingActivity();
    
    // When
    WorkflowExecutionDetails details = workflowExecutionService.getExecutionDetails(workflowId);
    
    // Then
    assertThat(details.currentActivity()).isNotNull();
    assertThat(details.currentActivity().name()).isEqualTo("EvaluateSTPActivity");
    assertThat(details.currentActivity().status()).isEqualTo("RUNNING");
    assertThat(details.activityTimeline()).hasSize(2);
}
```

## Phase 2: Frontend Implementation (Week 3-4)
### Week 3: Core Components
**Day 1-2: Enhanced API Client**
```typescript
// Update api/client.ts
export const api = {
  // ... existing methods ...
  
  getWorkflowExecutionDetails: (workflowId: string) => 
    client.get(`/backoffice/transactions/${workflowId}/execution-details`)
      .then(r => r.data as WorkflowExecutionDetails),
      
  getWorkflowExecutionDetailsRealTime: (workflowId: string) => {
    // WebSocket or polling implementation
    return new Observable<WorkflowExecutionDetails>(observer => {
      const interval = setInterval(() => {
        api.getWorkflowExecutionDetails(workflowId)
          .then(details => observer.next(details))
          .catch(error => observer.error(error));
      }, 5000); // 5-second polling
      
      return () => clearInterval(interval);
    });
  }
};
```
**Day 3-4: WorkflowCard Component**
```typescript
// New component: WorkflowCard.tsx
interface WorkflowCardProps {
  workflow: EnhancedWorkflowItem;
  onViewDetails: (workflowId: string) => void;
}
export const WorkflowCard: React.FC<WorkflowCardProps> = ({ workflow, onViewDetails }) => {
  const progress = calculateWorkflowProgress(workflow.activities);
  const currentActivity = getCurrentActivity(workflow.activities);
  
  return (
    <div className="workflow-card">
      <div className="workflow-header">
        <TransactionTypeIcon type={workflow.transactionType} />
        <div className="workflow-info">
          <h3>{getTransactionTypeLabel(workflow.transactionType)}</h3>
          <div className="workflow-meta">
            <span className="workflow-id">{workflow.workflowId}</span>
            <span className="transaction-id">{workflow.transactionId}</span>
          </div>
        </div>
        <div className="workflow-status">
          <StatusBadge status={workflow.status} />
          {workflow.elapsedTime && <span className="elapsed-time\">{workflow.elapsedTime}</span>}
        </div>
      </div>
      
      <div className="workflow-progress">
        <ActivityProgressBar 
n          activities={workflow.activities}
          currentActivity={currentActivity}
          progress={progress}
        />
n      </div>
      
      <div className="workflow-details">
        <div className="amount\">MYR {workflow.amount?.toFixed(2)}</div>
        {currentActivity && (\n          <div className="current-activity">
n            <Icon name="Play" size={14} />
n            <span>{currentActivity.name}</span>
n            <span className="activity-status\">{currentActivity.status}</span>
n          </div>
n        )}\n      </div>
      
      <div className="workflow-actions\">
        <Button variant=\"outline\" size=\"sm\" onClick={() => onViewDetails(workflow.workflowId)}>
n          <Icon name=\"Eye\" size={14} /> Details
        </Button>
n      </div>
n    </div>\n  );
n};
```
**Day 5: ActivityTimelineModal Component**
```typescript
// New component: ActivityTimelineModal.tsx
interface ActivityTimelineModalProps {
  workflowId: string;
  isOpen: boolean;
  onClose: () => void;
}
export const ActivityTimelineModal: React.FC<ActivityTimelineModalProps> = ({
  workflowId,
  isOpen,
  onClose
}) => {
  const [autoRefresh, setAutoRefresh] = useState(true);
n  \n  const { data: executionDetails, isLoading, error } = useQuery({
n    queryKey: ['workflowExecution', workflowId],
n    queryFn: () => api.getWorkflowExecutionDetails(workflowId),
n    refetchInterval: autoRefresh ? 5000 : false,
n    enabled: isOpen
  });\n\n  if (!isOpen) return null;\n\n  return (\n    <Modal isOpen={isOpen} onClose={onClose} size=\"xl\" className=\"execution-modal\">\n      <ModalHeader>\n        <div className=\"modal-header-content\">\n          <div className=\"workflow-title\">\n            <TransactionTypeIcon type={executionDetails?.transactionType} />
n            <h2>Workflow Execution Details</h2>\n          </div>\n          <div className=\"header-actions\">\n            <Toggle\n              checked={autoRefresh}\n              onChange={setAutoRefresh}\n              label=\"Auto-refresh\"\n            />\n            <Button variant=\"ghost\" size=\"sm\" onClick={() => queryClient.invalidateQueries(['workflowExecution', workflowId])}>
n              <Icon name=\"RefreshCw\" />\n            </Button>
n          </div>\n        </div>\n      </ModalHeader>\n      \n      <ModalBody>\n        {isLoading ? (\n          <LoadingSpinner />\n        ) : error ? (\n          <ErrorMessage error={error} />
n        ) : executionDetails ? (\n          <div className=\"execution-content\">\n            <ExecutionSummary details={executionDetails} />\n            <ActivityTimeline timeline={executionDetails.activityTimeline} />
n            {executionDetails.currentActivity && (\n              <CurrentActivityPanel activity={executionDetails.currentActivity} />
n            )}\n            <ExternalServiceStatus services={executionDetails.externalServiceStatus} />
n          </div>
n        ) : null}\n      </ModalBody>
n      \n      <ModalFooter>\n        <Button variant=\"outline\" onClick={onClose}>Close</Button>\n        <Button variant=\"primary\" onClick={() => handleCreateCase(workflowId)}>
n          Create Resolution Case
        </Button>
n      </ModalFooter>
n    </Modal>\n  );
n};
```

### Week 4: Advanced Features & Styling

**Day 1-2: Activity Timeline Component**
```typescript
// New component: ActivityTimeline.tsx
interface ActivityTimelineProps {
  timeline: ActivityTimelineItem[];
  currentActivity?: ActivityDetails;
}
export const ActivityTimeline: React.FC<ActivityTimelineProps> = ({ timeline, currentActivity }) => {
  return (\n    <div className=\"activity-timeline\">\n      <h3>Activity Execution Timeline</h3>
n      <div className=\"timeline-container\">\n        {timeline.map((item, index) => (\n          <ActivityTimelineItem\n            key={index}\n            item={item}\n            isCurrent={currentActivity?.name === item.name}
n            isLast={index === timeline.length - 1}
n          />\n        ))}\n      </div>\n    </div>\n  );\n};\n\nconst ActivityTimelineItem: React.FC<{ item: ActivityTimelineItem; isCurrent: boolean; isLast: boolean }> = ({
n  item,\n  isCurrent,\n  isLast\n}) => {\n  return (\n    <div className={`timeline-item ${isCurrent ? 'current' : ''} ${item.status.toLowerCase()}`}>\n      <div className=\"timeline-marker\">\n        <ActivityStatusIcon status={item.status} />\n      </div>\n      <div className=\"timeline-content\">\n        <div className=\"activity-header\">\n          <span className=\"activity-name\">{item.name}</span>\n          <span className=\"activity-duration\">{item.duration}</span>\n        </div>\n        <div className=\"activity-time\">{item.startTime}</div>\n        {item.pendingReason && (\n          <div className=\"pending-reason\">\n            <Icon name=\"AlertTriangle\" size={14} />\n            <span>{item.pendingReason}</span>\n          </div>\n        )}\n        {isCurrent && (\n          <div className=\"current-indicator\">\n            <span className=\"pulse\"></span>\n            <span>Currently executing</span>\n          </div>\n        )}\n      </div>\n      {!isLast && <div className=\"timeline-connector\"></div>}\n    </div>\n  );\n};
```

**Day 3-4: Styling & Responsive Design**
```css
// New styles: workflow-enhancements.scss
.workflow-card {
  background: white;
n  border-radius: 12px;\n  padding: 20px;\n  margin-bottom: 16px;\n  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);\n  transition: all 0.2s ease;\n  \n  &:hover {\n    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);\n    transform: translateY(-2px);\n  }\n  \n  .workflow-header {\n    display: flex;\n    align-items: center;\n    justify-content: space-between;\n    margin-bottom: 16px;\n    \n    .workflow-info h3 {\n      margin: 0;\n      font-size: 18px;\n      font-weight: 600;\n    }\n    \n    .workflow-meta {\n      display: flex;\n      gap: 12px;\n      font-size: 13px;\n      color: #64748b;\n      \n      .workflow-id, .transaction-id {\n        font-family: monospace;\n      }\n    }\n  }\n  \n  .workflow-progress {\n    margin-bottom: 16px;\n  }\n  \n  .activity-progress-bar {\n    height: 8px;\n    background: #e2e8f0;\n    border-radius: 4px;\n    overflow: hidden;\n    position: relative;\n    \n    .progress-fill {\n      height: 100%;\n      background: linear-gradient(90deg, #3b82f6, #06b6d4);\n      border-radius: 4px;\n      transition: width 0.3s ease;\n    }\n    \n    .current-activity-indicator {\n      position: absolute;\n      top: -2px;\n      width: 12px;\n      height: 12px;\n      background: #f59e0b;\n      border-radius: 50%;\n      animation: pulse 2s infinite;\n    }\n  }\n  \n  .current-activity {\n    display: flex;\n    align-items: center;\n    gap: 8px;\n    padding: 8px 12px;\n    background: #fef3c7;\n    border-radius: 8px;\n    font-size: 14px;\n    \n    .activity-status {\n      font-weight: 500;\n      color: #92400e;\n    }\n  }\n}\n\n.activity-timeline {\n  .timeline-container {\n    position: relative;\n    padding: 20px 0;\n  }\n  \n  .timeline-item {\n    display: flex;\n    align-items: flex-start;\n    position: relative;\n    margin-bottom: 24px;\n    \n    &.current {\n      .timeline-marker {\n        animation: pulse 2s infinite;\n      }\n    }\n    \n    &.completed .timeline-marker {\n      background: #10b981;\n      border-color: #10b981;\n    }\n    \n    &.running .timeline-marker {\n      background: #f59e0b;\n      border-color: #f59e0b;\n    }\n    \n    &.failed .timeline-marker {\n      background: #ef4444;\n      border-color: #ef4444;\n    }\n    \n    .timeline-marker {\n      width: 32px;\n      height: 32px;\n      border-radius: 50%;\n      background: #e2e8f0;\n      border: 3px solid #e2e8f0;\n      display: flex;\n      align-items: center;\n      justify-content: center;\n      flex-shrink: 0;\n      z-index: 2;\n    }\n    \n    .timeline-content {\n      flex: 1;\n      margin-left: 16px;\n      \n      .activity-header {\n        display: flex;\n        justify-content: space-between;\n        align-items: center;\n        margin-bottom: 4px;\n        \n        .activity-name {\n          font-weight: 600;\n          font-size: 15px;\n        }\n        \n        .activity-duration {\n          font-size: 13px;\n          color: #64748b;\n        }\n      }\n      \n      .pending-reason {\n        display: flex;\n        align-items: center;\n        gap: 6px;\n        margin-top: 8px;\n        padding: 8px 12px;\n        background: #fef3c7;\n        border-radius: 6px;\n        font-size: 13px;\n        color: #92400e;\n      }\n    }\n    \n    .timeline-connector {\n      position: absolute;\n      left: 15px;\n      top: 32px;\n      width: 2px;\n      height: calc(100% + 24px);\n      background: #e2e8f0;\n    }\n  }\n}\n\n@keyframes pulse {\n  0% {\n    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.7);\n  }\n  70% {\n    box-shadow: 0 0 0 10px rgba(245, 158, 11, 0);\n  }\n  100% {\n    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0);\n  }\n}\n\n// Responsive design\n@media (max-width: 768px) {\n  .workflow-card {\n    padding: 16px;\n    \n    .workflow-header {\n      flex-direction: column;\n      align-items: flex-start;\n      gap: 12px;\n    }\n    \n    .workflow-meta {\n      flex-direction: column;\n      gap: 4px;\n    }\n  }\n  \n  .activity-timeline {\n    .timeline-item {\n      .timeline-content {\n        margin-left: 12px;\n        \n        .activity-header {\n          flex-direction: column;\n          align-items: flex-start;\n          gap: 4px;\n        }\n      }\n    }\n  }\n}\n
```
**Day 5: Integration Testing**
- Test component integration with API calls
- Verify auto-refresh functionality
- Test responsive design across devices
- Performance testing with large datasets

## Phase 3: Advanced Features (Week 5-6)
### Week 5: External Service Integration
**Day 1-2: External Service Status Component**
```typescript
// New component: ExternalServiceStatus.tsx
interface ExternalServiceStatusProps {
  services: ExternalServiceStatus;
}
export const ExternalServiceStatus: React.FC<ExternalServiceStatusProps> = ({ services }) => {
  const serviceItems = [
n    { key: 'rulesService', name: 'Rules Service', icon: 'Scale' },\n    { key: 'ledgerService', name: 'Ledger Service', icon: 'DollarSign' },\n    { key: 'switchAdapter', name: 'Switch Adapter', icon: 'CreditCard' },\n    { key: 'billerService', name: 'Biller Service', icon: 'FileText' }\n  ];\n\n  return (\n    <div className=\"external-service-status\">\n      <h3>External Service Status</h3>\n      <div className=\"service-grid\">\n        {serviceItems.map(service => (\n          <ServiceStatusCard\n            key={service.key}\n            name={service.name}\n            icon={service.icon}\n            status={services[service.key as keyof ExternalServiceStatus]}\n          />\n        ))}\n      </div>\n    </div>\n  );\n};\n\nconst ServiceStatusCard: React.FC<{ name: string; icon: string; status: string }> = ({\n  name,\n  icon,\n  status\n}) => {\n  const statusConfig = {\n    AVAILABLE: { color: '#10b981', icon: 'CheckCircle', label: 'Available' },\n    RESPONDING: { color: '#10b981', icon: 'CheckCircle', label: 'Responding' },\n    TIMEOUT: { color: '#f59e0b', icon: 'Clock', label: 'Timeout' },\n    FAILED: { color: '#ef4444', icon: 'XCircle', label: 'Failed' },\n    NOT_REQUIRED: { color: '#6b7280', icon: 'MinusCircle', label: 'Not Required' }\n  };\n\n  const config = statusConfig[status as keyof typeof statusConfig] || statusConfig.NOT_REQUIRED;\n\n  return (\n    <div className={`service-card ${status.toLowerCase()}`}>\n      <div className=\"service-icon\">\n        <Icon name={icon} size={24} />\n      </div>\n      <div className=\"service-info\">\n        <div className=\"service-name\">{name}</div>\n        <div className=\"service-status\">\n          <Icon name={config.icon} color={config.color} size={16} />\n          <span>{config.label}</span>\n        </div>\n      </div>\n    </div>\n  );\n};
```

**Day 3-4: Debug Information Panel**
```typescript
// New component: DebugInformation.tsx
interface DebugInformationProps {
  details: Map<string, object>;\n}
export const DebugInformation: React.FC<DebugInformationProps> = ({ details }) => {
n  const [expanded, setExpanded] = useState(false);\n\n  return (\n    <div className=\"debug-information\">\n      <Collapsible\n        title=\"Debug Information\"\n        expanded={expanded}\n        onToggle={setExpanded}\n      >\n        <div className=\"debug-content\">\n          {Object.entries(details).map(([key, value]) => (\n            <DebugInfoRow\n              key={key}\n              label={key}\n              value={value}\n              depth={0}\n            />\n          ))}\n        </div>\n        <div className=\"debug-actions\">\n          <Button\n            variant=\"outline\"\n            size=\"sm\"\n            onClick={() => copyDebugInfo(details)}\n          >\n            <Icon name=\"Copy\" size={14} /> Copy Debug Info\n          </Button>\n          <Button\n            variant=\"outline\"\n            size=\"sm\"\n            onClick={() => exportDebugInfo(details)}\n          >\n            <Icon name=\"Download\" size={14} /> Export JSON\n          </Button>\n        </div>\n      </Collapsible>\n    </div>\n  );\n};\n\nconst DebugInfoRow: React.FC<{ label: string; value: any; depth: number }> = ({\n  label,\n  value,\n  depth\n}) => {\n  const [expanded, setExpanded] = useState(depth === 0);\n\n  if (typeof value === 'object' && value !== null) {\n    return (\n      <div className={`debug-row depth-${depth}`}>\n        <div\n          className=\"debug-label expandable\"\n          onClick={() => setExpanded(!expanded)}\n        >\n          <Icon name={expanded ? 'ChevronDown' : 'ChevronRight'} size={14} />\n          <span>{label}</span>\n        </div>\n        {expanded && (\n          <div className=\"debug-children\">\n            {Object.entries(value).map(([childKey, childValue]) => (\n              <DebugInfoRow\n                key={childKey}\n                label={childKey}\n                value={childValue}\n                depth={depth + 1}\n              />\n            ))}\n          </div>\n        )}\n      </div>\n    );\n  }\n\n  return (\n    <div className={`debug-row depth-${depth}`}>\n      <div className=\"debug-label\">{label}:</div>\n      <div className=\"debug-value\">\n        {typeof value === 'string' ? value : JSON.stringify(value)}\n      </div>\n    </div>\n  );\n};
```

**Day 5: Activity Input/Output Display**
```typescript
// Enhanced ActivityTimelineItem component
interface ActivityDetailsProps {
  activity: ActivityTimelineItem;\n  isExpanded: boolean;\n  onToggle: () => void;\n}
const ActivityDetails: React.FC<ActivityDetailsProps> = ({
n  activity,\n  isExpanded,\n  onToggle\n}) => {
n  return (\n    <div className=\"activity-details\">\n      <div className=\"activity-summary\" onClick={onToggle}>\n        <div className=\"activity-basic-info\">\n          <ActivityStatusIcon status={activity.status} />\n          <span className=\"activity-name\">{activity.name}</span>\n          <span className=\"activity-duration\">{activity.duration}</span>\n        </div>\n        <Icon name={isExpanded ? 'ChevronUp' : 'ChevronDown'} size={16} />\n      </div>\n      \n      {isExpanded && (\n        <div className=\"activity-expanded\">\n          <div className=\"activity-section\">\n            <h4>Input Parameters</h4>\n            <CodeBlock language=\"json\" code={JSON.stringify(activity.input, null, 2)} />\n          </div>\n          \n          {activity.output && (\n            <div className=\"activity-section\">\n              <h4>Output Results</h4>\n              <CodeBlock language=\"json\" code={JSON.stringify(activity.output, null, 2)} />\n            </div>\n          )}\n          \n          {activity.pendingReason && (\n            <div className=\"activity-section\">\n              <h4>Pending Reason</h4>\n              <Alert type=\"warning\" message={activity.pendingReason} />\n            </div>\n          )}\n          \n          <div className=\"activity-actions\">\n            <Button\n              variant=\"outline\"\n              size=\"sm\"\n              onClick={() => copyActivityDetails(activity)}\n            >\n              <Icon name=\"Copy\" size={14} /> Copy Details\n            </Button>\n          </div>\n        </div>\n      )}\n    </div>\n  );\n};\n\nconst CodeBlock: React.FC<{ language: string; code: string }> = ({ language, code }) => {\n  const [copied, setCopied] = useState(false);\n\n  const handleCopy = async () => {\n    await navigator.clipboard.writeText(code);\n    setCopied(true);\n    setTimeout(() => setCopied(false), 2000);\n  };\n\n  return (\n    <div className=\"code-block\">\n      <div className=\"code-header\">\n        <span className=\"language\">{language}</span>\n        <Button\n          variant=\"ghost\"\n          size=\"sm\"\n          onClick={handleCopy}\n        >\n          <Icon name={copied ? 'Check' : 'Copy'} size={14} />\n          {copied ? 'Copied!' : 'Copy'}\n        </Button>\n      </div>\n      <pre className=\"code-content\">\n        <code>{code}</code>\n      </pre>\n    </div>\n  );\n};
```

### Week 6: Performance & Polish

**Day 1-2: Performance Optimization**
```typescript
// Implement virtual scrolling for large timelines
import { FixedSizeList } from 'react-window';
const VirtualizedActivityTimeline: React.FC<{ items: ActivityTimelineItem[] }> = ({ items }) => {
  const Row = ({ index, style }: { index: number; style: React.CSSProperties }) => (
    <div style={style}>
      <ActivityTimelineItem item={items[index]} />
    </div>
  );
  return (\n    <FixedSizeList\n      height={400}\n      itemCount={items.length}\n      itemSize={80}\n      width=\"100%\"\n    >\n      {Row}\n    </FixedSizeList>\n  );\n};
// Implement debounced updates
const useDebouncedValue = <T,>(value: T, delay: number): T => {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);
    return () => {
      clearTimeout(handler);\n    };\n  }, [value, delay]);
  return debouncedValue;\n};
// Usage in components
const debouncedExecutionDetails = useDebouncedValue(executionDetails, 300);
```

**Day 3-4: Error Handling & Fallbacks**
```typescript
// Enhanced error handling with retry logic
const useWorkflowExecutionWithRetry = (workflowId: string, options?: { maxRetries?: number; retryDelay?: number }) => {
n  const [retryCount, setRetryCount] = useState(0);\n  const maxRetries = options?.maxRetries ?? 3;\n  const retryDelay = options?.retryDelay ?? 1000;\n\n  const query = useQuery({\n    queryKey: ['workflowExecution', workflowId, retryCount],\n    queryFn: async () => {\n      try {\n        return await api.getWorkflowExecutionDetails(workflowId);\n      } catch (error) {\n        if (retryCount < maxRetries) {\n          setTimeout(() => setRetryCount(prev => prev + 1), retryDelay);\n        }\n        throw error;\n      }\n    },\n    retry: maxRetries,\n    retryDelay: retryDelay,\n  });\n\n  return {\n    ...query,\n    retryCount,\n    canRetry: retryCount < maxRetries,\n    retry: () => setRetryCount(prev => prev + 1)\n  };\n};\n\n// Fallback UI for when Temporal is unavailable\nconst FallbackExecutionDetails: React.FC<{ workflowId: string; error: Error }> = ({ workflowId, error }) => {\n  return (\n    <div className=\"fallback-execution-details\">\n      <div className=\"fallback-header\">\n        <Icon name=\"AlertTriangle\" size={48} color=\"#f59e0b\" />\n        <h3>Enhanced Details Temporarily Unavailable</h3>\n      </div>\n      <div className=\"fallback-content\">\n        <p>We're unable to retrieve the detailed execution timeline at the moment.</p>\n        <p>Error: {error.message}</p>\n        <div className=\"fallback-actions\">\n          <Button onClick={() => window.location.reload()}>\n            Refresh Page\n          </Button>\n          <Button variant=\"outline\" onClick={() => window.open(`/transactions/${workflowId}/status`, '_blank')}>\n            View Basic Status\n          </Button>\n        </div>\n      </div>\n    </div>\n  );\n};
```

**Day 5: Final Integration Testing**
- Complete end-to-end testing with real workflows
- Performance testing with 100+ concurrent workflows
- Accessibility testing with screen readers
- Cross-browser compatibility verification

## Testing Strategy
### Unit Testing
```typescript
// Test for ActivityTimeline component
describe('ActivityTimeline', () => {
  it('should render timeline items in correct order', () => {
    const timeline = [
      { sequence: 1, name: 'CheckVelocity', status: 'COMPLETED' },
n      { sequence: 2, name: 'EvaluateSTP', status: 'RUNNING' }
n    ];
n    \n    render(<ActivityTimeline timeline={timeline} />);\n    \n    expect(screen.getByText('CheckVelocity')).toBeInTheDocument();\n    expect(screen.getByText('EvaluateSTP')).toBeInTheDocument();\n    expect(screen.getByText('Currently executing')).toBeInTheDocument();\n  });\n  \n  it('should highlight current activity', () => {
n    const timeline = [\n      { sequence: 1, name: 'CheckVelocity', status: 'COMPLETED' },\n      { sequence: 2, name: 'EvaluateSTP', status: 'RUNNING' }\n    ];\n    const currentActivity = { name: 'EvaluateSTP', status: 'RUNNING' };\n    \n    render(<ActivityTimeline timeline={timeline} currentActivity={currentActivity} />);\n    \n    const currentItem = screen.getByText('EvaluateSTP').closest('.timeline-item');\n    expect(currentItem).toHaveClass('current');\n  });\n});
```

### Integration Testing
```typescript
// Test API integration
describe('WorkflowExecutionDetails API', () => {
  it('should fetch execution details successfully', async () => {\n    const mockResponse = {\n      workflowId: 'WF-TEST-123',\n      currentStatus: 'RUNNING',\n      currentActivity: {\n        name: 'EvaluateSTPActivity',\n        status: 'RUNNING'\n      },\n      activityTimeline: [\n        { sequence: 1, name: 'CheckVelocity', status: 'COMPLETED' }\n      ]\n    };\n    \n    server.use(\n      rest.get('/api/v1/backoffice/transactions/WF-TEST-123/execution-details', \n        (req, res, ctx) => res(ctx.json(mockResponse))\n      )\n    );\n    \n    const result = await api.getWorkflowExecutionDetails('WF-TEST-123');\n    \n    expect(result.workflowId).toBe('WF-TEST-123');\n    expect(result.currentActivity.name).toBe('EvaluateSTPActivity');\n  });\n});
```

### E2E Testing
```typescript
// Test complete user workflow
describe('Workflow Troubleshooting E2E', () => {
  it('should allow admin to troubleshoot pending transaction', async () => {\n    // 1. Navigate to workflows page\n    await page.goto('/workflows');\n    \n    // 2. Find pending workflow\n    await page.waitForSelector('[data-testid="workflow-card"]');\n    const pendingCard = await page.locator('.workflow-card:has(.status-pending)').first();\n    \n    // 3. Click view details\n    await pendingCard.locator('button:has-text(\"Details\")').click();\n    \n    // 4. Verify modal opens with timeline\n    await page.waitForSelector('.activity-timeline');\n    \n    // 5. Check current activity is highlighted\n    const currentActivity = await page.locator('.timeline-item.current');\n    await expect(currentActivity).toBeVisible();\n    \n    // 6. Verify auto-refresh is working\n    await page.waitForTimeout(6000); // Wait for refresh\n    const updatedTime = await page.locator('.elapsed-time').textContent();\n    expect(updatedTime).toMatch(/\\d+s/); // Should show seconds elapsed\n  });\n});
```

## Deployment Plan

### Pre-deployment Checklist
- [ ] All unit tests passing (>90% coverage)
- [ ] Integration tests passing
- [ ] E2E tests passing
- [ ] Performance benchmarks met
- [ ] Accessibility audit passed
- [ ] Security review completed
- [ ] Documentation updated

### Deployment Steps
1. Backend Deployment:
   - Deploy new WorkflowExecutionService
   - Update ResolutionController with new endpoint
   - Verify Temporal connectivity
2. Frontend Deployment:
   - Deploy new React components
   - Update API client with new methods
   - Deploy enhanced styling
3. Post-deployment Verification:
   - Smoke test with sample workflows
   - Monitor performance metrics
   - Verify real-time updates working
   - Check error handling and fallbacks

### Rollback Plan
- Keep existing workflow detail modal as fallback
- Feature flag to toggle between old/new views
- Database changes are additive (no breaking changes)
- API versioning for backward compatibility

This comprehensive implementation plan provides a clear roadmap for delivering the enhanced Orchestrator Workflows page that solves the critical "pending transaction visibility" problem while maintaining high quality and performance standards.