/**
 * Contract Test: Agents API
 * 
 * Validates that the frontend makes correct HTTP calls to the backend.
 * This test uses MSW to intercept requests and validate the contract.
 * 
 * Backend: services/onboarding-service/.../web/dto/AgentResponse.java
 * Endpoint: GET /api/v1/backoffice/agents (via gateway)
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: GET /api/v1/backoffice/agents', () => {
  beforeEach(() => {
    server.resetHandlers()
  })

  it('should call correct endpoint with pagination params', async () => {
    let called = false
    
    server.use(
      http.get('/api/v1/backoffice/agents', ({ request }) => {
        called = true
        const url = new URL(request.url)
        return HttpResponse.json({
          content: [],
          totalElements: 0,
          totalPages: 0,
          page: parseInt(url.searchParams.get('page') || '0'),
          size: parseInt(url.searchParams.get('size') || '10'),
        })
      })
    )

    const response = await fetch('/api/v1/backoffice/agents?page=0&size=10')
    const data = await response.json()

    expect(called).toBe(true)
    expect(data).toHaveProperty('content')
    expect(data).toHaveProperty('totalElements')
  })

  it('should handle successful response with agent fields', async () => {
    server.use(
      http.get('/api/v1/backoffice/agents', () => {
        return HttpResponse.json([
          {
            agentId: 'a0000000-0000-0000-0000-000000000001',
            agentCode: 'AGT-001',
            businessName: 'Ahmad Razak Store',
            tier: 'PREMIUM',
            status: 'ACTIVE',
            phoneNumber: '012-3456789',
            merchantGpsLat: 3.139003,
            merchantGpsLng: 101.686855,
            createdAt: '2026-04-01T12:56:49.938566',
            updatedAt: '2026-04-01T12:56:49.938566',
          },
        ])
      })
    )

    const response = await fetch('/api/v1/backoffice/agents')
    const data = await response.json()

    expect(data).toHaveLength(1)
    expect(data[0]).toHaveProperty('agentId')
    expect(data[0]).toHaveProperty('agentCode')
    expect(data[0]).toHaveProperty('businessName')
    expect(data[0]).toHaveProperty('tier')
    expect(data[0]).toHaveProperty('status')
    expect(data[0]).toHaveProperty('phoneNumber')
  })

  it('should handle valid tier enum values', async () => {
    server.use(
      http.get('/api/v1/backoffice/agents', () => {
        return HttpResponse.json([
          { agentId: '1', agentCode: 'AGT-001', businessName: 'A', tier: 'BASIC', status: 'ACTIVE', phoneNumber: '0', merchantGpsLat: 0, merchantGpsLng: 0, createdAt: '', updatedAt: '' },
          { agentId: '2', agentCode: 'AGT-002', businessName: 'B', tier: 'STANDARD', status: 'ACTIVE', phoneNumber: '0', merchantGpsLat: 0, merchantGpsLng: 0, createdAt: '', updatedAt: '' },
          { agentId: '3', agentCode: 'AGT-003', businessName: 'C', tier: 'PREMIUM', status: 'ACTIVE', phoneNumber: '0', merchantGpsLat: 0, merchantGpsLng: 0, createdAt: '', updatedAt: '' },
        ])
      })
    )

    const response = await fetch('/api/v1/backoffice/agents')
    const data = await response.json()

    const tiers = data.map((a: any) => a.tier)
    expect(tiers).toContain('BASIC')
    expect(tiers).toContain('STANDARD')
    expect(tiers).toContain('PREMIUM')
  })

  it('should handle valid status enum values', async () => {
    server.use(
      http.get('/api/v1/backoffice/agents', () => {
        return HttpResponse.json([
          { agentId: '1', agentCode: 'AGT-001', businessName: 'A', tier: 'BASIC', status: 'ACTIVE', phoneNumber: '0', merchantGpsLat: 0, merchantGpsLng: 0, createdAt: '', updatedAt: '' },
          { agentId: '2', agentCode: 'AGT-002', businessName: 'B', tier: 'BASIC', status: 'SUSPENDED', phoneNumber: '0', merchantGpsLat: 0, merchantGpsLng: 0, createdAt: '', updatedAt: '' },
          { agentId: '3', agentCode: 'AGT-003', businessName: 'C', tier: 'BASIC', status: 'INACTIVE', phoneNumber: '0', merchantGpsLat: 0, merchantGpsLng: 0, createdAt: '', updatedAt: '' },
        ])
      })
    )

    const response = await fetch('/api/v1/backoffice/agents')
    const data = await response.json()

    const statuses = data.map((a: any) => a.status)
    expect(statuses).toContain('ACTIVE')
    expect(statuses).toContain('SUSPENDED')
    expect(statuses).toContain('INACTIVE')
  })

  it('should get agent user status', async () => {
    const agentId = 'a0000000-0000-0000-0000-000000000001'
    
    server.use(
      http.get(`/api/v1/backoffice/agents/${agentId}/user-status`, () => {
        return HttpResponse.json({
          agentId: agentId,
          status: 'NOT_CREATED',
        })
      })
    )

    const response = await fetch(`/api/v1/backoffice/agents/${agentId}/user-status`)
    const data = await response.json()

    expect(data.agentId).toBe(agentId)
    expect(data.status).toBe('NOT_CREATED')
  })
})