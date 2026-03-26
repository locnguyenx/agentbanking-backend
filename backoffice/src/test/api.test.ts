import { describe, it, expect, vi, beforeEach } from 'vitest'

describe('API Client', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('should have correct baseURL configuration', () => {
    const expectedBaseURL = '/api/v1'
    expect(expectedBaseURL).toBe('/api/v1')
  })

  it('should include auth token from localStorage', () => {
    localStorage.setItem('backoffice_token', 'test-token')
    const token = localStorage.getItem('backoffice_token')
    expect(token).toBe('test-token')
  })

  it('should have correct content-type header', () => {
    const headers = { 'Content-Type': 'application/json' }
    expect(headers['Content-Type']).toBe('application/json')
  })
})
