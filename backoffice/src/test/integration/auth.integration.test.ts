/**
 * Integration Test: Auth/Login API
 * 
 * Tests the login endpoint against REAL backend services (no MSW).
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

describe('Auth/Login API Integration Tests (Real Backend)', () => {
  beforeAll(() => {
    // Disable MSW so requests go to real backend
    server.close()
  })

  afterAll(() => {
    // Re-enable MSW for other tests
    server.listen()
  })

  it('should return access_token with valid credentials', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/auth/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'password' }),
    })

    expect(response.ok).toBe(true)
    const data = await response.json()

    expect(data).toHaveProperty('access_token')
    expect(typeof data.access_token).toBe('string')
    expect(data.access_token.length).toBeGreaterThan(0)

    expect(data).toHaveProperty('expires_in')
    expect(typeof data.expires_in).toBe('number')
    expect(data.expires_in).toBeGreaterThan(0)

    expect(data).toHaveProperty('token_type', 'Bearer')
  })

  it('should return 400 with error on invalid credentials', async () => {
    const response = await fetch(`${GATEWAY_URL}/api/v1/auth/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'WrongPassword123!' }),
    })

    expect([400, 401]).toContain(response.status)
    const data = await response.json()

    expect(data.status).toBe('FAILED')
    expect(data.error.code).toMatch(/ERR_AUTH|ERR_VAL/)
    expect(data.error).toHaveProperty('message')
  })
})
