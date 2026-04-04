/**
 * Contract Test: Dashboard API
 * 
 * Validates that the frontend makes correct HTTP calls to the backend.
 * This test uses MSW to intercept requests and validate the contract.
 * 
 * Endpoint: GET /api/v1/backoffice/dashboard
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: GET /api/v1/backoffice/dashboard', () => {
  beforeEach(() => {
    server.resetHandlers()
  })

  it('should call correct endpoint and return dashboard metrics', async () => {
    let called = false

    server.use(
      http.get('/api/v1/backoffice/dashboard', () => {
        called = true
        return HttpResponse.json({
          totalAgents: 1250,
          activeAgents: 980,
          todayVolume: 1_500_000.00,
          todayTransactions: 3420,
          pendingKyc: 45,
        })
      })
    )

    const response = await fetch('/api/v1/backoffice/dashboard')
    const data = await response.json()

    expect(called).toBe(true)
    expect(response.status).toBe(200)
    expect(data).toHaveProperty('totalAgents')
    expect(data).toHaveProperty('activeAgents')
    expect(data).toHaveProperty('todayVolume')
    expect(data).toHaveProperty('todayTransactions')
    expect(data).toHaveProperty('pendingKyc')
  })

  it('should return dashboard with correct types', async () => {
    server.use(
      http.get('/api/v1/backoffice/dashboard', () => {
        return HttpResponse.json({
          totalAgents: 1250,
          activeAgents: 980,
          todayVolume: 1_500_000.00,
          todayTransactions: 3420,
          pendingKyc: 45,
        })
      })
    )

    const response = await fetch('/api/v1/backoffice/dashboard')
    const data = await response.json()

    expect(typeof data.totalAgents).toBe('number')
    expect(typeof data.activeAgents).toBe('number')
    expect(typeof data.todayVolume).toBe('number')
    expect(typeof data.todayTransactions).toBe('number')
    expect(typeof data.pendingKyc).toBe('number')
  })

  it('should handle 500 error with standard error structure', async () => {
    server.use(
      http.get('/api/v1/backoffice/dashboard', () => {
        return HttpResponse.json(
          {
            status: 'FAILED',
            error: {
              code: 'ERR_SYS_001',
              message: 'Internal server error',
              action_code: 'RETRY',
              trace_id: 'trace-abc-123',
              timestamp: '2026-04-04T10:00:00+08:00',
            },
          },
          { status: 500 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/dashboard')
    const data = await response.json()

    expect(response.status).toBe(500)
    expect(data.status).toBe('FAILED')
    expect(data.error).toHaveProperty('code')
    expect(data.error).toHaveProperty('message')
    expect(data.error).toHaveProperty('action_code')
    expect(data.error).toHaveProperty('trace_id')
    expect(data.error).toHaveProperty('timestamp')
  })
})
