import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import { setupOrchestratorMocks, cleanupOrchestratorMocks } from '../mocks/orchestrator-mocks'
import { WorkflowExecutionDetails, ActivityStatus } from '../../types/workflow'

describe('Workflow Execution Details API Integration', () => {
  beforeAll(() => {
    setupOrchestratorMocks()
  })

  afterAll(() => {
    cleanupOrchestratorMocks()
  })

  describe('GET /api/v1/backoffice/transactions/{workflowId}/execution-details', () => {
    it('should return workflow execution details for running workflow', async () => {
      // Mock a running workflow with temporal history
      const mockResponse: WorkflowExecutionDetails = {
        workflowId: 'WF-RUNNING-123',
        currentStatus: 'RUNNING',
        currentActivity: {
          name: 'EvaluateSTPActivity',
          status: ActivityStatus.RUNNING,
          startTime: '2026-04-11T10:30:00Z',
          elapsedTime: '2.1s',
          retryAttempt: 1,
          maxRetries: 3,
          input: { amount: 1000, agentTier: 'PREMIER' }
        },
        activityTimeline: [
          {
            sequence: 1,
            name: 'CheckVelocityActivity',
            status: ActivityStatus.COMPLETED,
            startTime: '2026-04-11T10:29:58Z',
            duration: '0.3s',
            input: { agentId: 'AGT-001', amount: 1000 },
            output: { passed: true, limit: 5000, remaining: 4000 }
          },
          {
            sequence: 2,
            name: 'EvaluateSTPActivity',
            status: ActivityStatus.RUNNING,
            startTime: '2026-04-11T10:30:00Z',
            duration: '2.1s',
            input: { amount: 1000, agentTier: 'PREMIER' }
          }
        ],
        externalServiceStatus: {
          rulesService: 'RESPONDING',
          ledgerService: 'AVAILABLE',
          switchAdapter: 'AVAILABLE',
          billerService: 'NOT_REQUIRED'
        },
        estimatedCompletion: '2026-04-11T10:30:30Z',
        debugInfo: {
          temporalWorkflowId: 'WF-RUNNING-123',
          historyEventCount: 6,
          namespace: 'default'
        }
      }

      // Mock API response
      server.use(
        rest.get('/api/v1/backoffice/transactions/WF-RUNNING-123/execution-details', 
          (req, res, ctx) => {
            return res(ctx.json(mockResponse))
          }
        )
      )

      // Test the API call
      const response = await fetch('/api/v1/backoffice/transactions/WF-RUNNING-123/execution-details')
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data).toEqual(mockResponse)
      expect(data.currentStatus).toBe('RUNNING')
      expect(data.currentActivity.name).toBe('EvaluateSTPActivity')
      expect(data.activityTimeline).toHaveLength(2)
      expect(data.externalServiceStatus.rulesService).toBe('RESPONDING')
    })

    it('should return completed workflow details', async () => {
      const mockResponse: WorkflowExecutionDetails = {
        workflowId: 'WF-COMPLETED-456',
        currentStatus: 'COMPLETED',
        currentActivity: undefined,
        activityTimeline: [
          {
            sequence: 1,
            name: 'CheckVelocityActivity',
            status: ActivityStatus.COMPLETED,
            startTime: '2026-04-11T10:30:00Z',
            duration: '0.2s',
            input: { agentId: 'AGT-002', amount: 500 },
            output: { passed: true }
          },
          {
            sequence: 2,
            name: 'EvaluateSTPActivity',
            status: ActivityStatus.COMPLETED,
            startTime: '2026-04-11T10:30:01Z',
            duration: '1.1s',
            input: { amount: 500, agentTier: 'STANDARD' },
            output: { approved: true, category: 'FULL_STP' }
          },
          {
            sequence: 3,
            name: 'BlockFloatActivity',
            status: ActivityStatus.COMPLETED,
            startTime: '2026-04-11T10:30:02Z',
            duration: '0.5s',
            input: { agentId: 'AGT-002', amount: 500 },
            output: { transactionId: 'TX-123', referenceNumber: 'REF-456' }
          }
        ],
        externalServiceStatus: {
          rulesService: 'RESPONDING',
          ledgerService: 'RESPONDING',
          switchAdapter: 'RESPONDING',
          billerService: 'NOT_REQUIRED'
        },
        estimatedCompletion: undefined,
        debugInfo: {
          temporalWorkflowId: 'WF-COMPLETED-456',
          historyEventCount: 8
        }
      }

      server.use(
        rest.get('/api/v1/backoffice/transactions/WF-COMPLETED-456/execution-details', 
          (req, res, ctx) => {
            return res(ctx.json(mockResponse))
          }
        )
      )

      const response = await fetch('/api/v1/backoffice/transactions/WF-COMPLETED-456/execution-details')
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.currentStatus).toBe('COMPLETED')
      expect(data.currentActivity).toBeUndefined()
      expect(data.activityTimeline).toHaveLength(3)
      expect(data.activityTimeline.every(item => item.status === 'COMPLETED')).toBe(true)
    })

    it('should return failed workflow details with error information', async () => {
      const mockResponse: WorkflowExecutionDetails = {
        workflowId: 'WF-FAILED-789',
        currentStatus: 'FAILED',
        currentActivity: {
          name: 'AuthorizeAtSwitchActivity',
          status: ActivityStatus.FAILED,
          startTime: '2026-04-11T10:30:05Z',
          elapsedTime: '30.2s',
          retryAttempt: 3,
          maxRetries: 3,
          input: { pan: '4111111111111111', amount: 750 },
          errorMessage: 'Switch timeout: No response within 30 seconds'
        },
        activityTimeline: [
          {
            sequence: 1,
            name: 'CheckVelocityActivity',
            status: ActivityStatus.COMPLETED,
            startTime: '2026-04-11T10:30:00Z',
            duration: '0.2s',
            input: { agentId: 'AGT-003', amount: 750 },
            output: { passed: true }
          },
          {
            sequence: 2,
            name: 'EvaluateSTPActivity',
            status: ActivityStatus.COMPLETED,
            startTime: '2026-04-11T10:30:01Z',
            duration: '1.1s',
            input: { amount: 750, agentTier: 'PREMIER' },
            output: { approved: true }
          },
          {
            sequence: 3,
            name: 'AuthorizeAtSwitchActivity',
            status: ActivityStatus.FAILED,
            startTime: '2026-04-11T10:30:05Z',
            duration: '30.2s',
            input: { pan: '4111111111111111', amount: 750 },
            pendingReason: 'Switch timeout: No response within 30 seconds'
          }
        ],
        externalServiceStatus: {
          rulesService: 'RESPONDING',
          ledgerService: 'RESPONDING',
          switchAdapter: 'TIMEOUT',
          billerService: 'NOT_REQUIRED'
        },
        estimatedCompletion: undefined,
        debugInfo: {
          temporalWorkflowId: 'WF-FAILED-789',
          historyEventCount: 6,
          recentEvents: [
            { eventType: 'ACTIVITY_TASK_FAILED', eventTime: '2026-04-11T10:30:35Z', eventId: '7' }
          ]
        }
      }

      server.use(
        rest.get('/api/v1/backoffice/transactions/WF-FAILED-789/execution-details', 
          (req, res, ctx) => {
            return res(ctx.json(mockResponse))
          }
        )
      )

      const response = await fetch('/api/v1/backoffice/transactions/WF-FAILED-789/execution-details')
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.currentStatus).toBe('FAILED')
      expect(data.currentActivity.status).toBe('FAILED')
      expect(data.currentActivity.errorMessage).toContain('Switch timeout')
      expect(data.externalServiceStatus.switchAdapter).toBe('TIMEOUT')
    })

    it('should handle workflow not found (404)', async () => {
      server.use(
        rest.get('/api/v1/backoffice/transactions/WF-NOTFOUND-999/execution-details', 
          (req, res, ctx) => {
            return res(ctx.status(404), ctx.json({
              status: 'FAILED',
              error: {
                code: 'ERR_SYS_WORKFLOW_NOT_FOUND',
                message: 'Workflow not found',
                action_code: 'RETRY'
              }
            }))
          }
        )
      )

      const response = await fetch('/api/v1/backoffice/transactions/WF-NOTFOUND-999/execution-details')
      
      expect(response.status).toBe(404)
      const errorData = await response.json()
      expect(errorData.error.code).toBe('ERR_SYS_WORKFLOW_NOT_FOUND')
    })

    it('should handle server errors (500)', async () => {
      server.use(
        rest.get('/api/v1/backoffice/transactions/WF-ERROR-500/execution-details', 
          (req, res, ctx) => {
            return res(ctx.status(500), ctx.json({
              status: 'FAILED',
              error: {
                code: 'ERR_SYS_EXECUTION_DETAILS_FAILED',
                message: 'Failed to retrieve workflow execution details: Temporal connection failed',
                action_code: 'RETRY'
              }
            }))
          }
        )
      )

      const response = await fetch('/api/v1/backoffice/transactions/WF-ERROR-500/execution-details')
      
      expect(response.status).toBe(500)
      const errorData = await response.json()
      expect(errorData.error.code).toBe('ERR_SYS_EXECUTION_DETAILS_FAILED')
    })

    it('should handle external service failures correctly', async () => {
      const mockResponse: WorkflowExecutionDetails = {
        workflowId: 'WF-SERVICE-FAIL-001',
        currentStatus: 'FAILED',
        currentActivity: {
          name: 'BlockFloatActivity',
          status: ActivityStatus.FAILED,
          startTime: '2026-04-11T10:30:03Z',
          elapsedTime: '5.1s',
          retryAttempt: 1,
          maxRetries: 3,
          input: { agentId: 'AGT-004', amount: 300 },
          errorMessage: 'Ledger service unavailable'
        },
        activityTimeline: [
          {
            sequence: 1,
            name: 'CheckVelocityActivity',
            status: ActivityStatus.COMPLETED,
            startTime: '2026-04-11T10:30:00Z',
            duration: '0.2s',
            input: { agentId: 'AGT-004', amount: 300 },
            output: { passed: true }
          },
          {
            sequence: 2,
            name: 'BlockFloatActivity',
            status: ActivityStatus.FAILED,
            startTime: '2026-04-11T10:30:03Z',
            duration: '5.1s',
            input: { agentId: 'AGT-004', amount: 300 },
            pendingReason: 'Ledger service unavailable'
          }
        ],
        externalServiceStatus: {
          rulesService: 'RESPONDING',
          ledgerService: 'FAILED',
          switchAdapter: 'AVAILABLE',
          billerService: 'NOT_REQUIRED'
        },
        estimatedCompletion: undefined,
        debugInfo: {
          temporalWorkflowId: 'WF-SERVICE-FAIL-001',
          historyEventCount: 4
        }
      }

      server.use(
        rest.get('/api/v1/backoffice/transactions/WF-SERVICE-FAIL-001/execution-details', 
          (req, res, ctx) => {
            return res(ctx.json(mockResponse))
          }
        )
      )

      const response = await fetch('/api/v1/backoffice/transactions/WF-SERVICE-FAIL-001/execution-details')
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.externalServiceStatus.ledgerService).toBe('FAILED')
      expect(data.currentActivity.errorMessage).toContain('Ledger service unavailable')
    })

    it('should handle workflows with timeout activities', async () => {
      const mockResponse: WorkflowExecutionDetails = {
        workflowId: 'WF-TIMEOUT-002',
        currentStatus: 'FAILED',
        currentActivity: {
          name: 'AuthorizeAtSwitchActivity',
          status: ActivityStatus.TIMED_OUT,
          startTime: '2026-04-11T10:30:10Z',
          elapsedTime: '35.2s',
          retryAttempt: 2,
          maxRetries: 3,
          input: { pan: '4111111111111111', amount: 800 },
          errorMessage: 'Activity timed out: TIMEOUT'
        },
        activityTimeline: [
          {
            sequence: 1,
            name: 'CheckVelocityActivity',
            status: ActivityStatus.COMPLETED,
            startTime: '2026-04-11T10:30:00Z',
            duration: '0.2s',
            input: { agentId: 'AGT-005', amount: 800 },
            output: { passed: true }
          },
          {
            sequence: 2,
            name: 'AuthorizeAtSwitchActivity',
            status: ActivityStatus.TIMED_OUT,
            startTime: '2026-04-11T10:30:10Z',
            duration: '35.2s',
            input: { pan: '4111111111111111', amount: 800 },
            pendingReason: 'Activity timed out: TIMEOUT'
          }
        ],
        externalServiceStatus: {
          rulesService: 'RESPONDING',
          ledgerService: 'RESPONDING',
          switchAdapter: 'TIMEOUT',
          billerService: 'NOT_REQUIRED'
        },
        estimatedCompletion: undefined,
        debugInfo: {
          temporalWorkflowId: 'WF-TIMEOUT-002',
          historyEventCount: 5
        }
      }

      server.use(
        rest.get('/api/v1/backoffice/transactions/WF-TIMEOUT-002/execution-details', 
          (req, res, ctx) => {
            return res(ctx.json(mockResponse))
          }
        )
      )

      const response = await fetch('/api/v1/backoffice/transactions/WF-TIMEOUT-002/execution-details')
      const data = await response.json()

      expect(response.status).toBe(200)
      expect(data.currentActivity.status).toBe('TIMED_OUT')
      expect(data.externalServiceStatus.switchAdapter).toBe('TIMEOUT')
    })
  })

  describe('API Response Validation', () => {
    it('should validate required fields in response', async () => {
      const mockResponse = {
        workflowId: 'WF-VALIDATION-003',
        currentStatus: 'RUNNING',
        currentActivity: {
          name: 'TestActivity',
          status: 'RUNNING',
          startTime: '2026-04-11T10:30:00Z',
          elapsedTime: '1.0s',
          retryAttempt: 1,
          maxRetries: 3,
          input: {}
        },
        activityTimeline: [],
        externalServiceStatus: {
          rulesService: 'AVAILABLE',
          ledgerService: 'AVAILABLE',
          switchAdapter: 'AVAILABLE',
          billerService: 'NOT_REQUIRED'
        },
        estimatedCompletion: null,
        debugInfo: {}
      }

      server.use(
        rest.get('/api/v1/backoffice/transactions/WF-VALIDATION-003/execution-details', 
          (req, res, ctx) => {
            return res(ctx.json(mockResponse))
          }
        )
      )

      const response = await fetch('/api/v1/backoffice/transactions/WF-VALIDATION-003/execution-details')
      const data = await response.json()

      // Verify all required fields are present
      expect(data).toHaveProperty('workflowId')
      expect(data).toHaveProperty('currentStatus')
      expect(data).toHaveProperty('activityTimeline')
      expect(data).toHaveProperty('externalServiceStatus')
      expect(data).toHaveProperty('debugInfo')
      
      // Verify external service status has all required services
      expect(data.externalServiceStatus).toHaveProperty('rulesService')
      expect(data.externalServiceStatus).toHaveProperty('ledgerService')
      expect(data.externalServiceStatus).toHaveProperty('switchAdapter')
      expect(data.externalServiceStatus).toHaveProperty('billerService')
    })
  })
})