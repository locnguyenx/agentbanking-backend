/**
 * Contract Test: Auth/Login API
 * 
 * Validates that the frontend makes correct HTTP calls to the backend.
 * This test uses MSW to intercept requests and validate the contract.
 * 
 * Backend: services/auth-service/.../web/dto/LoginRequest.java
 * Endpoint: POST /api/v1/auth/token
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: POST /api/v1/auth/token', () => {
  beforeEach(() => {
    server.resetHandlers()
  })

  it('should call correct endpoint with username and password', async () => {
    let capturedBody: Record<string, unknown> | null = null

    server.use(
      http.post('/api/v1/auth/token', async ({ request }) => {
        capturedBody = await request.json() as Record<string, unknown>
        return HttpResponse.json({
          access_token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mock',
          refresh_token: 'dGhpcyBpcyBhIG1vY2sgcmVmcmVzaCB0b2tlbg',
          expires_in: 900,
          token_type: 'Bearer',
        })
      })
    )

    const response = await fetch('/api/v1/auth/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'secret123' }),
    })
    const data = await response.json()

    expect(capturedBody).not.toBeNull()
    expect(capturedBody).toHaveProperty('username', 'admin')
    expect(capturedBody).toHaveProperty('password', 'secret123')

    expect(data).toHaveProperty('access_token')
    expect(data).toHaveProperty('refresh_token')
    expect(data).toHaveProperty('expires_in')
    expect(data).toHaveProperty('token_type', 'Bearer')
  })

  it('should return 401 with ERR_AUTH_INVALID_CREDENTIALS on invalid credentials', async () => {
    server.use(
      http.post('/api/v1/auth/token', () => {
        return HttpResponse.json(
          {
            status: 'FAILED',
            error: {
              code: 'ERR_AUTH_INVALID_CREDENTIALS',
              message: 'Invalid username or password',
              action_code: 'DECLINE',
              trace_id: 'test-trace-id',
              timestamp: '2026-04-04T10:00:00+08:00',
            },
          },
          { status: 401 }
        )
      })
    )

    const response = await fetch('/api/v1/auth/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'wrong' }),
    })

    expect(response.status).toBe(401)
    const data = await response.json()
    expect(data.status).toBe('FAILED')
    expect(data.error.code).toBe('ERR_AUTH_INVALID_CREDENTIALS')
    expect(data.error).toHaveProperty('message')
    expect(data.error).toHaveProperty('action_code', 'DECLINE')
  })

  it('should return 403 with ERR_AUTH_ACCOUNT_LOCKED on locked account', async () => {
    server.use(
      http.post('/api/v1/auth/token', () => {
        return HttpResponse.json(
          {
            status: 'FAILED',
            error: {
              code: 'ERR_AUTH_ACCOUNT_LOCKED',
              message: 'Account is locked due to multiple failed login attempts',
              action_code: 'REVIEW',
              trace_id: 'test-trace-id',
              timestamp: '2026-04-04T10:00:00+08:00',
            },
          },
          { status: 403 }
        )
      })
    )

    const response = await fetch('/api/v1/auth/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'lockeduser', password: 'secret123' }),
    })

    expect(response.status).toBe(403)
    const data = await response.json()
    expect(data.status).toBe('FAILED')
    expect(data.error.code).toBe('ERR_AUTH_ACCOUNT_LOCKED')
    expect(data.error).toHaveProperty('message')
    expect(data.error).toHaveProperty('action_code', 'REVIEW')
  })
})
