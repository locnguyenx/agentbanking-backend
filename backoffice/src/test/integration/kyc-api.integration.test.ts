/**
 * Integration Test: KYC API
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
 * - KYC review queue returns valid data
 * - Error handling matches actual backend behavior
 */
import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import { server } from '../mocks/server'

const GATEWAY_URL = process.env.TEST_GATEWAY_URL || 'http://localhost:8080'

describe('KYC API Integration Tests (Real Backend)', () => {
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

  it('should fetch KYC review queue from real backend', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/kyc/review-queue`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    // Backend returns paginated object { content: [...], totalElements, ... }
    expect(data).toHaveProperty('content')
    expect(Array.isArray(data.content)).toBe(true)
  })

  it('should return valid KYC review items with required properties', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/kyc/review-queue`, {
      headers: { Authorization: `Bearer ${authToken}` },
    })

    expect(response.ok).toBe(true)
    const data = await response.json()
    
    // Handle both empty queue and populated queue
    const items = data.content || []
    if (items.length > 0) {
      const item = items[0]
      expect(item).toHaveProperty('verificationId')
      expect(item).toHaveProperty('fullName')
      expect(item).toHaveProperty('amlStatus')
    }
  })

  it('should handle 401 unauthorized without token', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/backoffice/kyc/review-queue`)
    
    expect(response.status).toBe(401)
    const data = await response.json()
    expect(data.status).toBe('FAILED')
    expect(data.error.code).toContain('ERR_AUTH')
  })
})
