/**
 * Integration Test: Settlement API
 * 
 * Tests the settlement endpoints against REAL backend services (no MSW).
 * 
 * Prerequisites:
 * 1. Docker Compose must be running (docker compose up -d)
 * 2. Services must be healthy
 * 
 * Run with: npm run test:integration
 * 
 * This test verifies:
 * - Settlement summary returns correct schema
 * - Settlement export endpoint responds correctly
 */
import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import { server } from '../mocks/server'

const GATEWAY_URL = process.env.TEST_GATEWAY_URL || 'http://localhost:8080'

describe('Settlement API Integration Tests (Real Backend)', () => {
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

  describe('GET /api/v1/backoffice/settlement', () => {
    it('should return settlement summary with required fields', async () => {
      const today = new Date().toISOString().split('T')[0]
      const response = await fetch(
        `${GATEWAY_URL}/api/v1/backoffice/settlement?date=${today}`,
        {
          headers: { Authorization: `Bearer ${authToken}` },
        }
      )

      expect(response.ok).toBe(true)
      const data = await response.json()
      expect(data).toHaveProperty('totalDeposits')
      expect(data).toHaveProperty('totalWithdrawals')
      expect(data).toHaveProperty('totalCommissions')
      expect(data).toHaveProperty('netAmount')
      expect(typeof data.totalDeposits).toBe('number')
      expect(typeof data.totalWithdrawals).toBe('number')
    })

    it('should return settlement for a specific historical date', async () => {
      const response = await fetch(
        `${GATEWAY_URL}/api/v1/backoffice/settlement?date=2026-01-15`,
        {
          headers: { Authorization: `Bearer ${authToken}` },
        }
      )

      expect(response.ok).toBe(true)
      const data = await response.json()
      expect(data).toHaveProperty('totalDeposits')
      expect(data).toHaveProperty('totalWithdrawals')
      expect(data).toHaveProperty('totalCommissions')
      expect(data).toHaveProperty('netAmount')
    })

    it('should return 401 without auth token', async () => {
      const response = await fetch(
        `${GATEWAY_URL}/api/v1/backoffice/settlement?date=2026-01-15`
      )

      expect(response.status).toBe(401)
      const data = await response.json()
      expect(data.status).toBe('FAILED')
      expect(data.error.code).toContain('ERR_AUTH')
    })
  })

  describe('GET /api/v1/backoffice/settlement/export', () => {
    it('should return OK response for export', async () => {
      const today = new Date().toISOString().split('T')[0]
      const response = await fetch(
        `${GATEWAY_URL}/api/v1/backoffice/settlement/export?date=${today}`,
        {
          headers: { Authorization: `Bearer ${authToken}` },
        }
      )

      expect(response.ok).toBe(true)
    })

    it('should return OK response for historical date export', async () => {
      const response = await fetch(
        `${GATEWAY_URL}/api/v1/backoffice/settlement/export?date=2026-01-15`,
        {
          headers: { Authorization: `Bearer ${authToken}` },
        }
      )

      expect(response.ok).toBe(true)
    })

    it('should return 401 without auth token', async () => {
      const response = await fetch(
        `${GATEWAY_URL}/api/v1/backoffice/settlement/export?date=2026-01-15`
      )

      expect(response.status).toBe(401)
      const data = await response.json()
      expect(data.status).toBe('FAILED')
      expect(data.error.code).toContain('ERR_AUTH')
    })
  })
})
