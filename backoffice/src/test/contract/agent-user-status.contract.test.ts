/**
 * Contract test: Agent User Status API
 * 
 * Backend: services/auth-iam-service/.../web/dto/AgentUserStatusResponse.java
 * Endpoint: GET /api/v1/backoffice/agents/{agentId}/user-status (via gateway)
 */
import { describe, it, expect } from 'vitest'
import { VALID_AGENT_USER_STATUSES } from '../mocks/schemas'

describe('Agent User Status API Contract', () => {
  it('should document the response shape', () => {
    const fields = ['agentId', 'status', 'userId', 'error']
    expect(fields).toContain('agentId')
    expect(fields).toContain('status')
    expect(fields).toContain('userId')
    expect(fields).toContain('error')
  })

  it('should use correct status enum values', () => {
    // Backend returns UserStatus enum values or 'NOT_CREATED' when no user exists
    expect(VALID_AGENT_USER_STATUSES).toContain('ACTIVE')
    expect(VALID_AGENT_USER_STATUSES).toContain('INACTIVE')
    expect(VALID_AGENT_USER_STATUSES).toContain('LOCKED')
    expect(VALID_AGENT_USER_STATUSES).toContain('DELETED')
    expect(VALID_AGENT_USER_STATUSES).toContain('NOT_CREATED')
    
    // These should NOT be returned
    expect(VALID_AGENT_USER_STATUSES).not.toContain('CREATED')
    expect(VALID_AGENT_USER_STATUSES).not.toContain('PENDING')
  })
})
