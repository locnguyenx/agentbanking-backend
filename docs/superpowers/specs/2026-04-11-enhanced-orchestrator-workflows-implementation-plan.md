# Implementation Plan: Enhanced Orchestrator Workflows Page
*Date: 2026-04-11*

## Implementation Status

### ✅ COMPLETED - Backend
- [x] WorkflowExecutionService in orchestrator-service
- [x] API endpoint: `GET /api/v1/backoffice/transactions/{workflowId}/execution-details`
- [x] Response DTOs: WorkflowExecutionDetailsResponse, ActivityDetails, ActivityTimelineItem, ExternalServiceStatus

### ✅ COMPLETED - Frontend Components Created (NOT YET INTEGRATED)
- [x] WorkflowCard.tsx - Card view component
- [x] ActivityTimelineModal.tsx - Enhanced detail modal
- [x] Types: EnhancedWorkflowItem, ActivityDetails, etc. in workflow.ts

### ❌ IN PROGRESS - Frontend Integration
- [ ] OrchestratorWorkflows page: Add view mode toggle
- [ ] Integrate WorkflowCard component  
- [ ] Replace WorkflowDetailModal with ActivityTimelineModal
- [ ] Connect API data to components

---

## Overview
This document provides the detailed implementation plan for enhancing the Backoffice UI Orchestrator Workflows page to provide comprehensive workflow execution visibility for transaction troubleshooting.

## Problem Being Solved
- **Current Issue**: Transactions show as "PENDING" with generic reasons, administrators cannot see which specific workflow activity is currently executing
- **Business Impact**: Increased resolution time for transaction issues, higher support ticket volume, reduced operational efficiency
- **Solution**: Real-time workflow activity timeline with detailed execution status and external service monitoring

## Implementation Phases

### Phase 1: Backend Foundation (Week 1-2)
**Objective**: Create the core backend service for retrieving workflow execution details from Temporal

#### Week 1 Tasks
1. **Create WorkflowExecutionService** (Day 1-2)
   - New service class in orchestrator-service
   - Temporal integration for workflow history retrieval
   - Activity timeline parsing logic
   - External service status detection

2. **Create Response DTOs** (Day 3-4)
   - WorkflowExecutionDetailsResponse record
   - ActivityTimelineItem, ActivityDetails, ExternalServiceStatus
   - Proper JSON serialization support

3. **Update ResolutionController** (Day 5)
   - Add new GET endpoint: `/api/v1/backoffice/transactions/{workflowId}/execution-details`
   - Error handling and response mapping
   - Integration with existing controller methods

#### Week 2 Tasks
1. **Temporal Integration Testing** (Day 1-3)
   - Test with various workflow types
   - Verify activity timeline parsing accuracy
   - Error handling for edge cases

2. **Unit Tests & Documentation** (Day 4-5)
   - Comprehensive unit test coverage
   - Integration test scenarios
   - API documentation

### Phase 2: Frontend Core Components (Week 3-4)
**Objective**: Build the new React components for enhanced workflow visualization

#### Week 3 Tasks
1. **Enhanced API Client** (Day 1-2)
   - New methods for execution details endpoint
   - Real-time update mechanisms (polling/WebSocket)
   - Error handling and retry logic

2. **WorkflowCard Component** (Day 3-4)
   - Card-based layout replacing table rows
   - Activity progress bar visualization
   - Current activity highlighting
   - Responsive design implementation

3. **ActivityTimelineModal Component** (Day 5)
   - Modal for detailed workflow view
   - Auto-refresh functionality
   - Activity timeline visualization
   - Integration with existing modal patterns

#### Week 4 Tasks
1. **Activity Timeline Component** (Day 1-2)
   - Visual timeline with step indicators
   - Activity status icons and colors
   - Expandable activity details
   - Time tracking and duration display

2. **Styling & Responsive Design** (Day 3-4)
   - Modern card-based styling
   - Mobile-responsive layouts
   - Accessibility features
   - Performance optimizations

3. **Integration Testing** (Day 5)
   - Component integration verification
   - API call testing
   - Auto-refresh functionality

### Phase 3: Advanced Features (Week 5-6)
**Objective**: Add advanced troubleshooting features and performance optimizations

#### Week 5 Tasks
1. **External Service Status Component** (Day 1-2)
   - Real-time service health monitoring
   - Visual service status indicators
   - Integration with workflow execution

2. **Debug Information Panel** (Day 3-4)
   - Collapsible debug information
   - Activity input/output parameter display
   - JSON formatting and copy functionality
   - Export capabilities

3. **Activity Parameter Display** (Day 5)
   - Activity input/output visualization
   - Code block formatting
   - Copy to clipboard functionality
   - Error context display

#### Week 6 Tasks
1. **Performance Optimization** (Day 1-2)
   - Virtual scrolling for large timelines
   - Debounced updates
   - Memory leak prevention
   - Caching strategies

2. **Error Handling & Fallbacks** (Day 3-4)
   - Graceful degradation when Temporal unavailable
   - Retry mechanisms with exponential backoff
   - User-friendly error messages
   - Fallback to basic status display

3. **Final Integration Testing** (Day 5)
   - End-to-end testing with real workflows
   - Performance testing
   - Accessibility audit
   - Cross-browser compatibility

### Phase 4: Testing & Deployment (Week 7-8)
**Objective**: Comprehensive testing and production deployment

#### Week 7 Tasks
1. **Comprehensive Testing** (Day 1-3)
   - Unit test coverage >90%
   - Integration test scenarios
   - E2E testing with real workflows
   - Performance benchmarking

2. **Quality Assurance** (Day 4-5)
   - Accessibility testing
   - Security review
   - Code quality audit
   - Documentation review

#### Week 8 Tasks
1. **User Acceptance Testing** (Day 1-3)
   - Admin team testing
   - Feedback incorporation
   - Training material creation
   - Deployment preparation

2. **Production Deployment** (Day 4-5)
   - Staged deployment
   - Monitoring setup
   - Rollback procedures
   - Post-deployment verification

## Technical Specifications

### Backend API Contract
```
GET /api/v1/backoffice/transactions/{workflowId}/execution-details

Response:
{
  "workflowId": "string",
  "currentStatus": "string",
  "currentActivity": {
    "name": "string",
    "status": "string",
    "startTime": "string",
    "elapsedTime": "string",
    "retryAttempt": "number",
    "maxRetries": "number",
    "input": "object",
    "output": "object",
    "errorMessage": "string"
  },
  "activityTimeline": [
    {
      "sequence": "number",
      "name": "string",
      "status": "string",
      "startTime": "string",
      "duration": "string",
      "input": "object",
      "output": "object",
      "pendingReason": "string"
    }
  ],
  "externalServiceStatus": {
    "rulesService": "string",
    "ledgerService": "string",
    "switchAdapter": "string",
    "billerService": "string"
  },
  "estimatedCompletion": "string",
  "debugInfo": "object"
}
```

### Frontend Component Architecture
```
App
└── OrchestratorWorkflowsPage
    ├── WorkflowFilters
    ├── WorkflowListView
    │   ├── WorkflowCard (new)
    │   ├── WorkflowTableRow (existing)
    │   └── WorkflowTimelineView (new)
    ├── ActivityTimelineModal (new)
    │   ├── ExecutionSummary
    │   ├── ActivityTimeline
    │   ├── CurrentActivityPanel
    │   ├── ExternalServiceStatus
    │   └── DebugInformation
    └── PaginationControls
```

### Database Changes
- No schema changes required
- Leverages existing TransactionRecord table
- Uses Temporal for activity history storage

### Security Considerations
- Authentication via existing JWT tokens
- Authorization checks for backoffice users
- No sensitive data exposure in debug info
- Rate limiting on real-time updates

## Success Criteria

### Functional Requirements
- [ ] Real-time workflow activity execution status visible
- [ ] Pending transactions display current activity name and reason
- [ ] Activity timeline shows step-by-step execution progress
- [ ] External service status visible and accurate
- [ ] Auto-refresh updates activity status without page reload
- [ ] Activity details expandable for debugging information

### Performance Requirements
- [ ] Activity status updates within 5 seconds
- [ ] Modal loading time under 2 seconds
- [ ] Smooth UI updates without flickering
- [ ] Efficient memory usage with large workflow lists

### Usability Requirements
- [ ] Intuitive visual indicators for activity status
- [ ] Clear labeling of current workflow step
- [ ] Accessible keyboard navigation support
- [ ] Mobile-responsive design for all screen sizes

### Reliability Requirements
- [ ] Graceful handling of Temporal connection failures
- [ ] Fallback display when activity details unavailable
- [ ] Consistent state management across UI updates
- [ ] Proper error boundaries and user feedback

## Risk Mitigation

### Technical Risks
- **Temporal API Performance**: Implement caching and request throttling
- **Large Data Volumes**: Use virtual scrolling and pagination
- **Real-time Connection Stability**: Implement fallback polling

### Business Risks
- **User Adoption**: Provide toggle between old/new views
- **Training Requirements**: Create intuitive design with help tooltips
- **Deployment Issues**: Maintain rollback capabilities

## Dependencies
- Temporal cluster access and configuration
- Existing API infrastructure
- Admin user availability for UAT
- Development environment setup

## Estimated Effort
- **Timeline**: 8 weeks
- **Team**: 2 backend developers, 2 frontend developers, 1 QA engineer
- **Priority**: High (addresses critical operational issue)

## Next Steps
1. Review and approve this implementation plan
2. Set up development environment with Temporal integration
3. Create feature branch for development work
4. Begin Phase 1 implementation (Backend Foundation)

This implementation plan provides a clear, structured approach to delivering the enhanced Orchestrator Workflows page that will solve the critical pending transaction visibility problem.