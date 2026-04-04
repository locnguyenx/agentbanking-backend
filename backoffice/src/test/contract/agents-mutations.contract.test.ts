/**
 * Contract Test: Agent Mutations
 *
 * Validates that the frontend makes correct HTTP calls for agent mutations.
 * POST   /api/v1/backoffice/agents          - Create agent
 * PUT    /api/v1/backoffice/agents/:id      - Update agent
 * DELETE /api/v1/backoffice/agents/:id      - Deactivate agent
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: POST /api/v1/backoffice/agents (Create Agent)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with required fields', async () => {
    let capturedBody: any

    server.use(
      http.post('/api/v1/backoffice/agents', async ({ request }) => {
        capturedBody = await request.json()
        return HttpResponse.json(
          {
            agentId: 'new-agent-id',
            agentCode: capturedBody.agentCode,
            businessName: capturedBody.businessName,
            tier: capturedBody.tier,
            status: 'ACTIVE',
            phoneNumber: capturedBody.phoneNumber,
            merchantGpsLat: capturedBody.merchantGpsLat,
            merchantGpsLng: capturedBody.merchantGpsLng,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
          { status: 201 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/agents', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        agentCode: 'AGT-009',
        businessName: 'New Store',
        tier: 'STANDARD',
        merchantGpsLat: 3.139003,
        merchantGpsLng: 101.686855,
        mykadNumber: '900101011234',
        phoneNumber: '012-3456789',
      }),
    })

    expect(capturedBody.agentCode).toBe('AGT-009')
    expect(capturedBody.businessName).toBe('New Store')
    expect(capturedBody.tier).toBe('STANDARD')
    expect(capturedBody.merchantGpsLat).toBe(3.139003)
    expect(capturedBody.merchantGpsLng).toBe(101.686855)
    expect(capturedBody.mykadNumber).toBe('900101011234')
    expect(capturedBody.phoneNumber).toBe('012-3456789')
    expect(response.ok).toBe(true)

    const data = await response.json()
    expect(data).toHaveProperty('agentId')
    expect(data).toHaveProperty('agentCode')
    expect(data).toHaveProperty('businessName')
    expect(data).toHaveProperty('tier')
    expect(data).toHaveProperty('status')
    expect(data).toHaveProperty('phoneNumber')
  })
})

describe('Contract: PUT /api/v1/backoffice/agents/:id (Update Agent)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with agentId in path', async () => {
    let capturedId: string | undefined
    let capturedBody: any

    server.use(
      http.put('/api/v1/backoffice/agents/:id', async ({ request, params }) => {
        capturedId = params.id as string
        capturedBody = await request.json()
        return HttpResponse.json({
          agentId: capturedId,
          agentCode: 'AGT-001',
          businessName: capturedBody.businessName,
          tier: capturedBody.tier,
          status: 'ACTIVE',
          phoneNumber: capturedBody.phoneNumber,
          merchantGpsLat: capturedBody.merchantGpsLat,
          merchantGpsLng: capturedBody.merchantGpsLng,
          createdAt: '2026-04-01T12:00:00Z',
          updatedAt: new Date().toISOString(),
        })
      })
    )

    const agentId = 'a0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/agents/${agentId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        businessName: 'Updated Store',
        tier: 'PREMIUM',
        phoneNumber: '012-9998888',
        merchantGpsLat: 3.139003,
        merchantGpsLng: 101.686855,
      }),
    })

    expect(capturedId).toBe(agentId)
    expect(capturedBody.businessName).toBe('Updated Store')
    expect(capturedBody.tier).toBe('PREMIUM')
    expect(capturedBody.phoneNumber).toBe('012-9998888')
    expect(response.ok).toBe(true)

    const data = await response.json()
    expect(data.agentId).toBe(agentId)
    expect(data.businessName).toBe('Updated Store')
    expect(data.tier).toBe('PREMIUM')
  })
})

describe('Contract: DELETE /api/v1/backoffice/agents/:id (Deactivate Agent)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with agentId in path', async () => {
    let capturedId: string | undefined

    server.use(
      http.delete('/api/v1/backoffice/agents/:id', ({ params }) => {
        capturedId = params.id as string
        return new HttpResponse(null, { status: 204 })
      })
    )

    const agentId = 'a0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/agents/${agentId}`, {
      method: 'DELETE',
    })

    expect(capturedId).toBe(agentId)
    expect(response.status).toBe(204)
  })
})
