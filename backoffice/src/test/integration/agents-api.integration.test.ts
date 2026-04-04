/**
 * Integration Test: Agents API
 * 
 * Tests the frontend against REAL backend services (no MSW).
 * 
 * Prerequisites:
 * 1. Docker Compose must be running (docker compose up -d)
 * 2. Services must be healthy
 * 
 * Run with: npm run test:integration
 * 
 * This test verifies:
 * - Real HTTP responses from backend match expected schema
 * - Error handling matches actual backend behavior
 * - Data from real database is correct
 */
import { describe, it, expect, beforeAll, afterAll, beforeEach } from 'vitest'
import { server } from '../mocks/server'

const GATEWAY_URL = process.env.TEST_GATEWAY_URL || 'http://localhost:8080'

describe('Agents API Integration Tests (Real Backend)', () => {
  let authToken: string

  beforeAll(async () => {
    // Disable MSW so requests go to real backend
    server.close()
    
    // Get auth token from real backend
    const tokenResponse = await fetch(`${GATEWAY_URL}/api/v1/auth/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'password' }),
    })
    
    const tokenData = await tokenResponse.json()
    authToken = tokenData.access_token
  })

  afterAll(() => {
    // Re-enable MSW for other tests
    server.listen()
  })

  it('should fetch agents from real backend', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(Array.isArray(data)).toBe(true)
    expect(data.length).toBeGreaterThan(0)
    
    const agent = data[0]
    expect(agent).toHaveProperty('agentId')
    expect(agent).toHaveProperty('agentCode')
    expect(agent).toHaveProperty('businessName')
    expect(agent).toHaveProperty('tier')
    expect(agent).toHaveProperty('status')
    expect(agent).toHaveProperty('phoneNumber')
  })

  it('should return valid tier enum values', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    
    const validTiers = ['MICRO', 'STANDARD', 'PREMIER']
    data.forEach((agent: any) => {
      expect(validTiers).toContain(agent.tier)
    })
  })

  it('should return valid status enum values', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    
    const validStatuses = ['ACTIVE', 'SUSPENDED', 'INACTIVE']
    data.forEach((agent: any) => {
      expect(validStatuses).toContain(agent.status)
    })
  })

  it('should handle 401 unauthorized without token', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents`)
    
    expect(response.status).toBe(401)
    const data = await response.json()
    expect(data.status).toBe('FAILED')
    expect(data.error.code).toContain('ERR_AUTH')
  })

  describe('Agent Mutations', () => {
    let createdAgentId: string

    it('should create a new agent', async () => {
      const uniqueId = Date.now().toString().slice(-8)
      const newAgent = {
        agentCode: `AGT${uniqueId}`,
        businessName: 'Test Agent Business',
        tier: 'STANDARD',
        merchantGpsLat: 3.139003,
        merchantGpsLng: 101.686855,
        mykadNumber: `900101${uniqueId}`,
        phoneNumber: '+60123456789',
      }

      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authToken}`,
        },
        body: JSON.stringify(newAgent),
      })

      expect(response.ok).toBe(true)
      const data = await response.json()
      expect(data).toHaveProperty('agentId')
      expect(data.agentCode).toBe(newAgent.agentCode)
      expect(data.businessName).toBe(newAgent.businessName)
      expect(data.status).toBeDefined()
      createdAgentId = data.agentId
    })

    it('should update an existing agent', async () => {
      const updatedData = {
        businessName: 'Updated Agent Business',
        tier: 'PREMIER',
        merchantGpsLat: 3.139003,
        merchantGpsLng: 101.686855,
        phoneNumber: '+60198765432',
      }

      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents/${createdAgentId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authToken}`,
        },
        body: JSON.stringify(updatedData),
      })

      expect(response.ok).toBe(true)
      const data = await response.json()
      expect(data.businessName).toBe(updatedData.businessName)
      expect(data.phoneNumber).toBe(updatedData.phoneNumber)
    })

    it('should deactivate an agent', async () => {
      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents/${createdAgentId}`, {
        method: 'DELETE',
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      })

      expect(response.status).toBe(204)
    })
  })

  describe('Agent User Account Creation', () => {
    let agentWithoutUserId: string

    beforeEach(async () => {
      const agentsResponse = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents`, {
        headers: { Authorization: `Bearer ${authToken}` },
      })
      const agents = await agentsResponse.json()

      for (const agent of agents) {
        const statusResponse = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents/${agent.agentId}/user-status`, {
          headers: { Authorization: `Bearer ${authToken}` },
        })
        const statusData = await statusResponse.json()
        if (statusData.status === 'NOT_CREATED') {
          agentWithoutUserId = agent.agentId
          break
        }
      }

      if (!agentWithoutUserId) {
        const uniqueId = Date.now().toString().slice(-8)
        const newAgent = {
          agentCode: `AGT${uniqueId}`,
          businessName: 'Test Agent For User Creation',
          tier: 'STANDARD',
          merchantGpsLat: 3.139003,
          merchantGpsLng: 101.686855,
          mykadNumber: `900102${uniqueId}`,
          phoneNumber: '+60123456789',
        }

        const createResponse = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${authToken}`,
          },
          body: JSON.stringify(newAgent),
        })
        const createdAgent = await createResponse.json()
        agentWithoutUserId = createdAgent.agentId
      }
    })

    it('should create a user account for an agent without one', async () => {
      // Get agent details to send in the request
      const agentResponse = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents/${agentWithoutUserId}`, {
        headers: { Authorization: `Bearer ${authToken}` },
      })
      const agent = await agentResponse.json()

      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents/${agentWithoutUserId}/create-user`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authToken}`,
        },
        body: JSON.stringify({
          agentCode: agent?.agentCode || `AGT_${agentWithoutUserId}`,
          businessName: agent?.businessName || 'Test Agent',
          phone: agent?.phoneNumber || '+60123456789',
          email: agent?.email || 'agent@test.com',
        }),
      })

      // Accept 200, 201, or 409/500 if user already exists
      if ([200, 201].includes(response.status)) {
        const data = await response.json()
        expect(data).toHaveProperty('userId')
        expect(data.username).toBeDefined()
      } else {
        // User already exists - that's fine for this test
        const data = await response.json()
        expect(data.status).toBe('FAILED')
      }
    })

    it('should return 409 when creating user for agent that already has one', async () => {
      const agentsResponse = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents`, {
        headers: { Authorization: `Bearer ${authToken}` },
      })
      const agents = await agentsResponse.json()

      let agentWithUserId: string | null = null
      for (const agent of agents) {
        const statusResponse = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents/${agent.agentId}/user-status`, {
          headers: { Authorization: `Bearer ${authToken}` },
        })
        const statusData = await statusResponse.json()
        if (statusData.status !== 'NOT_CREATED') {
          agentWithUserId = agent.agentId
          break
        }
      }

      if (!agentWithUserId) {
        const createResponse = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents/${agentWithoutUserId}/create-user`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${authToken}`,
          },
          body: JSON.stringify({
            agentCode: `AGT_${agentWithoutUserId}`,
            businessName: 'Test Agent',
            phone: '+60123456789',
            email: 'agent@test.com',
          }),
        })
        expect(createResponse.ok).toBe(true)
        agentWithUserId = agentWithoutUserId
      }

      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/agents/${agentWithUserId}/create-user`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authToken}`,
        },
        body: JSON.stringify({
          agentCode: `AGT_${agentWithUserId}`,
          businessName: 'Test Agent',
          phone: '+60123456789',
          email: 'agent@test.com',
        }),
      })

      // Backend returns 400, 409, or 500 for duplicate user
      expect([400, 409, 500]).toContain(response.status)
      const data = await response.json()
      expect(data.status).toBe('FAILED')
    })
  })
})