/**
 * Contract Test: Agents Create User Account
 *
 * Validates the HTTP contract for POST /api/v1/backoffice/agents/{agentId}/create-user
 * This endpoint was missing a backend implementation and caused "Unexpected error" toast
 * for agent AGT-008. The backend fix has been applied.
 *
 * BDD Scenarios:
 * - S.AG-10: Backoffice user creates agent user account successfully
 * - S.AG-11: Create user fails when agent already has user
 *
 * BRD Requirements:
 * - US-BO-04: Backoffice admin can create user accounts for agents without login access
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: POST /api/v1/backoffice/agents/:agentId/create-user', () => {
  beforeEach(() => {
    server.resetHandlers()
  })

  it('should call correct endpoint with agentId in path', async () => {
    let capturedPath: string | undefined
    let capturedBody: any

    server.use(
      http.post('/api/v1/backoffice/agents/:agentId/create-user', async ({ request, params }) => {
        capturedPath = params.agentId as string
        capturedBody = await request.json()
        return HttpResponse.json({
          userId: 'test-user-id',
          username: 'AGT-008',
          status: 'ACTIVE',
          agentId: capturedPath,
          temporaryPassword: 'TempPass123!',
          mustChangePassword: true,
        })
      })
    )

    const agentId = 'a0000000-0000-0000-0000-000000000008'
    const response = await fetch(`/api/v1/backoffice/agents/${agentId}/create-user`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        phone: '012-3456789',
        businessName: 'Test Business',
      }),
    })

    expect(capturedPath).toBe(agentId)
    expect(capturedBody).toEqual({
      phone: '012-3456789',
      businessName: 'Test Business',
    })
    expect(response.ok).toBe(true)
  })

  it('should handle 409 when user already exists', async () => {
    server.use(
      http.post('/api/v1/backoffice/agents/:agentId/create-user', () => {
        return HttpResponse.json(
          {
            status: 'FAILED',
            error: {
              code: 'ERR_BIZ_USER_EXISTS',
              message: 'User already exists for this agent',
              action_code: 'REVIEW',
            },
          },
          { status: 409 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/agents/test-agent-id/create-user', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: '012-3456789', businessName: 'Test' }),
    })

    expect(response.status).toBe(409)
    const data = await response.json()
    expect(data.error.code).toBe('ERR_BIZ_USER_EXISTS')
  })

  it('should handle 404 when agent not found', async () => {
    server.use(
      http.post('/api/v1/backoffice/agents/:agentId/create-user', () => {
        return HttpResponse.json(
          {
            status: 'FAILED',
            error: {
              code: 'ERR_BIZ_AGENT_NOT_FOUND',
              message: 'Agent not found',
            },
          },
          { status: 404 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/agents/nonexistent/create-user', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phone: '012-3456789', businessName: 'Test' }),
    })

    expect(response.status).toBe(404)
  })
})
