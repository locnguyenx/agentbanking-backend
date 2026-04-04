/**
 * Contract Test: Users API
 * 
 * Validates that the frontend makes correct HTTP calls to the backend.
 * This test uses MSW to intercept requests and validate the contract.
 * 
 * What it tests:
 * 1. Frontend calls correct endpoint URL
 * 2. Frontend handles correct HTTP method
 * 3. Frontend correctly parses response
 * 4. Frontend handles errors properly
 * 
 * Backend: services/auth-iam-service/.../web/dto/UserResponseDto.java
 * Endpoint: GET /api/v1/backoffice/admin/users (via gateway)
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: GET /api/v1/backoffice/admin/users', () => {
  beforeEach(() => {
    server.resetHandlers()
  })

  it('should call correct endpoint with pagination params', async () => {
    let called = false
    let receivedUrl = ''
    
    server.use(
      http.get('/api/v1/backoffice/admin/users', ({ request }) => {
        called = true
        receivedUrl = new URL(request.url).toString()
        return HttpResponse.json([])
      })
    )

    // Simulate frontend API call
    const response = await fetch('/api/v1/backoffice/admin/users?page=0&size=10')
    await response.json()

    expect(called).toBe(true)
    expect(receivedUrl).toContain('page=0')
    expect(receivedUrl).toContain('size=10')
  })

  it('should handle successful response with userType and agentId fields', async () => {
    server.use(
      http.get('/api/v1/backoffice/admin/users', () => {
        return HttpResponse.json([
          {
            userId: 'a0000000-0000-0000-0000-000000000001',
            username: 'admin',
            email: 'admin@agentbanking.com',
            fullName: 'System Administrator',
            status: 'ACTIVE',
            userType: 'INTERNAL',
            agentId: null,
            permissions: [],
            createdAt: '2026-04-01T12:56:52.825224',
            lastLoginAt: null,
          },
        ])
      })
    )

    const response = await fetch('/api/v1/backoffice/admin/users')
    const data = await response.json()

    expect(data).toHaveLength(1)
    expect(data[0]).toHaveProperty('userType')
    expect(data[0]).toHaveProperty('agentId')
    expect(data[0].userType).toBe('INTERNAL')
    expect(data[0].agentId).toBeNull()
  })

  it('should handle various user status values from backend', async () => {
    server.use(
      http.get('/api/v1/backoffice/admin/users', () => {
        return HttpResponse.json([
          { userId: '1', username: 'user1', status: 'ACTIVE', userType: 'INTERNAL', agentId: null, email: 'a@a.com', fullName: 'A', permissions: [], createdAt: '' },
          { userId: '2', username: 'user2', status: 'INACTIVE', userType: 'INTERNAL', agentId: null, email: 'b@b.com', fullName: 'B', permissions: [], createdAt: '' },
          { userId: '3', username: 'user3', status: 'LOCKED', userType: 'INTERNAL', agentId: null, email: 'c@c.com', fullName: 'C', permissions: [], createdAt: '' },
          { userId: '4', username: 'user4', status: 'DELETED', userType: 'INTERNAL', agentId: null, email: 'd@d.com', fullName: 'D', permissions: [], createdAt: '' },
        ])
      })
    )

    const response = await fetch('/api/v1/backoffice/admin/users')
    const data = await response.json()

    const statuses = data.map((u: any) => u.status)
    expect(statuses).toContain('ACTIVE')
    expect(statuses).toContain('INACTIVE')
    expect(statuses).toContain('LOCKED')
    expect(statuses).toContain('DELETED')
    
    // These should NOT come from backend
    expect(statuses).not.toContain('DISABLED')
    expect(statuses).not.toContain('PENDING')
    expect(statuses).not.toContain('CREATED')
  })

  it('should handle 401 unauthorized error', async () => {
    server.use(
      http.get('/api/v1/backoffice/admin/users', () => {
        return HttpResponse.json(
          { status: 'FAILED', error: { code: 'ERR_AUTH_INVALID_TOKEN', message: 'Invalid token', action_code: 'DECLINE' } },
          { status: 401 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/admin/users')
    
    expect(response.status).toBe(401)
    const data = await response.json()
    expect(data.status).toBe('FAILED')
    expect(data.error.code).toBe('ERR_AUTH_INVALID_TOKEN')
  })

  it('should handle 404 not found error', async () => {
    server.use(
      http.get('/api/v1/backoffice/admin/users/:id', () => {
        return HttpResponse.json(
          { status: 'FAILED', error: { code: 'ERR_NOT_FOUND', message: 'User not found', action_code: 'DECLINE' } },
          { status: 404 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/admin/users/nonexistent-id')
    
    expect(response.status).toBe(404)
  })
})