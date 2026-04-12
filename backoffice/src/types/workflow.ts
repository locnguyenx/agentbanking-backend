// Base workflow item interface
export interface WorkflowItem {
  workflowId: string
  transactionId: string
  transactionType: string
  amount: number | null
  status: string
  createdAt: string
  pendingReason?: string
  agentId?: string
  customerFee?: number
  referenceNumber?: string
  errorCode?: string
  errorMessage?: string
  externalReference?: string
  errorDetails?: string
  completedAt?: string
  updatedAt?: string
}

// Enhanced workflow execution details types
export interface WorkflowExecutionDetails {
  workflowId: string
  currentStatus: string
  currentActivity?: ActivityDetails
  activityTimeline: ActivityTimelineItem[]
  externalServiceStatus: ExternalServiceStatus
  estimatedCompletion?: string
  debugInfo: Record<string, any>
}

export interface ActivityDetails {
  name: string
  status: string
  startTime: string
  elapsedTime: string
  retryAttempt: number
  maxRetries: number
  input: Record<string, any>
  output?: Record<string, any>
  errorMessage?: string
}

export interface ActivityTimelineItem {
  sequence: number
  name: string
  status: string
  startTime: string
  duration: string
  input: Record<string, any>
  output?: Record<string, any>
  pendingReason?: string
}

export interface ExternalServiceStatus {
  rulesService: string
  ledgerService: string
  switchAdapter: string
  billerService: string
}

// Activity status constants
export const ActivityStatus = {
  SCHEDULED: 'SCHEDULED',
  RUNNING: 'RUNNING',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED',
  TIMED_OUT: 'TIMED_OUT',
  CANCELLED: 'CANCELLED'
} as const

export const ServiceStatus = {
  AVAILABLE: 'AVAILABLE',
  RESPONDING: 'RESPONDING',
  TIMEOUT: 'TIMEOUT',
  FAILED: 'FAILED',
  NOT_REQUIRED: 'NOT_REQUIRED'
} as const

// Enhanced workflow item with activity data
export interface EnhancedWorkflowItem extends WorkflowItem {
  activities?: ActivityTimelineItem[]
  currentActivity?: ActivityDetails
  elapsedTime?: string
  externalServiceStatus?: ExternalServiceStatus
}