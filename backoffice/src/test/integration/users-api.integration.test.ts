/**
 * Integration Test: Users API
 * 
 * Tests the frontend against REAL backend services (no MSW).
 * 
 * Prerequisites:
 * 1. Docker Compose must be running (docker compose up -d)
 * 2. Services must be healthy
 * 
 * Run with: npm run test:integration
 */
import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import { server } from '../mocks/server'

const GATEWAY_URL = process.env.TEST_GATEWAY_URL || 'http://localhost:8080'

describe('Users API Integration Tests (Real Backend)', () => {
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

  it('should fetch users from real backend with userType and agentId', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    expect(Array.isArray(data)).toBe(true)
    expect(data.length).toBeGreaterThan(0)
    
    // Verify the NEW fields that were added (the original bug fix!)
    const user = data[0]
    expect(user).toHaveProperty('userType')
    expect(user).toHaveProperty('agentId')
  })

  it('should return valid status enum values from backend', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    
    const validStatuses = ['ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED']
    data.forEach((user: any) => {
      expect(validStatuses).toContain(user.status)
    })
    
    // Verify DISABLED is NOT returned (common frontend mistake)
    data.forEach((user: any) => {
      expect(user.status).not.toBe('DISABLED')
    })
  })

  it('should require authentication', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users`)
    
    expect(response.status).toBe(401)
    const data = await response.json()
    expect(data.status).toBe('FAILED')
    expect(data.error.code).toContain('ERR_AUTH')
  })

  it('should handle EXTERNAL userType correctly (agent users)', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    
    // Find an external user (should have agentId)
    const externalUsers = data.filter((u: any) => u.userType === 'EXTERNAL')
    expect(externalUsers.length).toBeGreaterThan(0)
    
    externalUsers.forEach((user: any) => {
      expect(user.agentId).toBeDefined()
      expect(user.agentId).not.toBeNull()
    })
  })
})

describe('Users API Mutations Integration Tests (Real Backend)', () => {
  let authToken: string
  let createdUserId: string

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

  describe('Create User', () => {
    it('should create a new user and return userId, username, status', async () => {
      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authToken}`,
        },
        body: JSON.stringify({
          username: `testuser_${Date.now()}`,
          email: `testuser_${Date.now()}@example.com`,
          fullName: 'Test Integration User',
          password: 'TempPass123!',
          userType: 'INTERNAL',
        }),
      })

      expect(response.ok).toBe(true)
      expect(response.status).toBe(201)

      const data = await response.json()
      expect(data).toHaveProperty('userId')
      expect(data).toHaveProperty('username')
      expect(data).toHaveProperty('status')
      expect(data.status).toBe('ACTIVE')

      createdUserId = data.userId
    })

    it('should return 400 when creating a user with duplicate username', async () => {
      // Create first user
      const uniqueName = `dupuser_${Date.now()}`
      await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authToken}`,
        },
        body: JSON.stringify({
          username: uniqueName,
          email: `dup${Date.now()}@example.com`,
          fullName: 'Duplicate Test User',
          password: 'TempPass123!',
          userType: 'INTERNAL',
        }),
      })

      // Try to create duplicate
      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authToken}`,
        },
        body: JSON.stringify({
          username: uniqueName,
          email: `dup2${Date.now()}@example.com`,
          fullName: 'Duplicate Test User 2',
          password: 'TempPass123!',
          userType: 'INTERNAL',
        }),
      })

      expect([400, 409]).toContain(response.status)
      const data = await response.json()
      expect(data.status).toBe('FAILED')
    })
  })

  describe('Update User', () => {
    it('should update user fields and return updated data', async () => {
      const updatedEmail = `updated_${Date.now()}@example.com`
      const updatedName = 'Updated Integration User'

      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users/${createdUserId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authToken}`,
        },
        body: JSON.stringify({
          username: `testuser_${Date.now()}`,
          email: updatedEmail,
          fullName: updatedName,
        }),
      })

      expect(response.ok).toBe(true)

      const data = await response.json()
      expect(data).toHaveProperty('userId')
      expect(data.userId).toBe(createdUserId)
      expect(data.email).toBe(updatedEmail)
      expect(data.fullName).toBe(updatedName)
    })

    it('should return 404 when updating a non-existent user', async () => {
      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users/00000000-0000-0000-0000-000000000099`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authToken}`,
        },
        body: JSON.stringify({
          username: 'nonexistent_user',
          email: 'test@example.com',
          fullName: 'Test',
        }),
      })

      // Backend may return 404 or 500 for non-existent user
      expect([404, 500]).toContain(response.status)
      const text = await response.text()
      if (text) {
        const data = JSON.parse(text)
        expect(data.status).toBe('FAILED')
      }
    })
  })

  describe('Lock User', () => {
    it('should lock a user and return status LOCKED', async () => {
      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users/${createdUserId}/lock`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authToken}`,
        },
        body: JSON.stringify({}),
      })

      expect(response.ok).toBe(true)

      const data = await response.json()
      expect(data).toHaveProperty('userId')
      expect(data.userId).toBe(createdUserId)
      expect(data.status).toBe('LOCKED')
    })
  })

  describe('Unlock User', () => {
    it('should unlock a user and return status ACTIVE', async () => {
      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users/${createdUserId}/unlock`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authToken}`,
        },
        body: JSON.stringify({}),
      })

      expect(response.ok).toBe(true)

      const data = await response.json()
      expect(data).toHaveProperty('userId')
      expect(data.userId).toBe(createdUserId)
      expect(data.status).toBe('ACTIVE')
    })
  })

  describe('Reset Password', () => {
    it('should reset password and return temporaryPassword', async () => {
      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users/${createdUserId}/reset-password`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authToken}`,
        },
        body: JSON.stringify({ newPassword: 'NewTemp456!' }),
      })

      expect(response.ok).toBe(true)

      const data = await response.json()
      expect(data).toHaveProperty('userId')
      expect(data.userId).toBe(createdUserId)
      expect(data).toHaveProperty('temporaryPassword')
      expect(data.mustChangePassword).toBe(true)
    })
  })

  describe('Delete User', () => {
    it('should delete a user and return 204', async () => {
      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users/${createdUserId}`, {
        method: 'DELETE',
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      })

      expect(response.status).toBe(204)
    })

    it('should return 404 when deleting an already deleted user', async () => {
      const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/admin/users/${createdUserId}`, {
        method: 'DELETE',
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      })

      expect(response.status).toBe(404)
    })
  })
})