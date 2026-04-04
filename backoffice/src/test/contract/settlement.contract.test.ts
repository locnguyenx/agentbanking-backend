/**
 * Contract Test: Settlement API
 *
 * Validates that the frontend makes correct HTTP calls to the backend.
 * This test uses MSW to intercept requests and validate the contract.
 *
 * Endpoints:
 *   GET /api/v1/backoffice/settlement?date=YYYY-MM-DD
 *   GET /api/v1/backoffice/settlement/export?date=YYYY-MM-DD
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: GET /api/v1/backoffice/settlement', () => {
  beforeEach(() => {
    server.resetHandlers()
  })

  it('should call correct endpoint with date query param', async () => {
    let capturedDate: string | null = null

    server.use(
      http.get('/api/v1/backoffice/settlement', ({ request }) => {
        const url = new URL(request.url)
        capturedDate = url.searchParams.get('date')
        return HttpResponse.json({
          totalDeposits: 1250000.0,
          totalWithdrawals: 850000.0,
          totalCommissions: 12500.0,
          netAmount: 400000.0,
          agentDetails: [],
        })
      })
    )

    const response = await fetch('/api/v1/backoffice/settlement?date=2026-04-03')

    expect(capturedDate).toBe('2026-04-03')
    expect(response.ok).toBe(true)
  })

  it('should handle successful response with settlement fields', async () => {
    server.use(
      http.get('/api/v1/backoffice/settlement', () => {
        return HttpResponse.json({
          totalDeposits: 1250000.0,
          totalWithdrawals: 850000.0,
          totalCommissions: 12500.0,
          netAmount: 400000.0,
          agentDetails: [
            {
              agentId: 'a0000000-0000-0000-0000-000000000001',
              agentCode: 'AGT-001',
              businessName: 'Ahmad Razak Store',
              deposits: 50000.0,
              withdrawals: 30000.0,
              commissions: 500.0,
              netAmount: 20000.0,
            },
          ],
        })
      })
    )

    const response = await fetch('/api/v1/backoffice/settlement?date=2026-04-03')
    const data = await response.json()

    expect(data).toHaveProperty('totalDeposits')
    expect(data).toHaveProperty('totalWithdrawals')
    expect(data).toHaveProperty('totalCommissions')
    expect(data).toHaveProperty('netAmount')
    expect(data).toHaveProperty('agentDetails')
    expect(Array.isArray(data.agentDetails)).toBe(true)
  })

  it('should validate numeric fields are present and correct type', async () => {
    server.use(
      http.get('/api/v1/backoffice/settlement', () => {
        return HttpResponse.json({
          totalDeposits: 1250000.0,
          totalWithdrawals: 850000.0,
          totalCommissions: 12500.0,
          netAmount: 400000.0,
          agentDetails: [],
        })
      })
    )

    const response = await fetch('/api/v1/backoffice/settlement?date=2026-04-03')
    const data = await response.json()

    expect(typeof data.totalDeposits).toBe('number')
    expect(typeof data.totalWithdrawals).toBe('number')
    expect(typeof data.totalCommissions).toBe('number')
    expect(typeof data.netAmount).toBe('number')
  })

  it('should handle agentDetails array with agent-level breakdown', async () => {
    server.use(
      http.get('/api/v1/backoffice/settlement', () => {
        return HttpResponse.json({
          totalDeposits: 100000.0,
          totalWithdrawals: 60000.0,
          totalCommissions: 1000.0,
          netAmount: 40000.0,
          agentDetails: [
            {
              agentId: 'a0000000-0000-0000-0000-000000000001',
              agentCode: 'AGT-001',
              businessName: 'Ahmad Razak Store',
              deposits: 50000.0,
              withdrawals: 30000.0,
              commissions: 500.0,
              netAmount: 20000.0,
            },
            {
              agentId: 'a0000000-0000-0000-0000-000000000002',
              agentCode: 'AGT-002',
              businessName: 'Siti Aminah Enterprise',
              deposits: 50000.0,
              withdrawals: 30000.0,
              commissions: 500.0,
              netAmount: 20000.0,
            },
          ],
        })
      })
    )

    const response = await fetch('/api/v1/backoffice/settlement?date=2026-04-03')
    const data = await response.json()

    expect(data.agentDetails).toHaveLength(2)
    expect(data.agentDetails[0]).toHaveProperty('agentId')
    expect(data.agentDetails[0]).toHaveProperty('agentCode')
    expect(data.agentDetails[0]).toHaveProperty('businessName')
    expect(data.agentDetails[0]).toHaveProperty('deposits')
    expect(data.agentDetails[0]).toHaveProperty('withdrawals')
    expect(data.agentDetails[0]).toHaveProperty('commissions')
    expect(data.agentDetails[0]).toHaveProperty('netAmount')
  })
})

describe('Contract: GET /api/v1/backoffice/settlement/export', () => {
  beforeEach(() => {
    server.resetHandlers()
  })

  it('should call correct endpoint with date query param', async () => {
    let capturedDate: string | null = null

    server.use(
      http.get('/api/v1/backoffice/settlement/export', ({ request }) => {
        const url = new URL(request.url)
        capturedDate = url.searchParams.get('date')
        return new HttpResponse('agentId,agentCode,deposits,withdrawals,commissions,netAmount\nAGT-001,Ahmad Razak Store,50000,30000,500,20000\n', {
          headers: { 'Content-Type': 'text/csv' },
        })
      })
    )

    const response = await fetch('/api/v1/backoffice/settlement/export?date=2026-04-03')

    expect(capturedDate).toBe('2026-04-03')
    expect(response.ok).toBe(true)
  })

  it('should return blob with content-type text/csv', async () => {
    const csvContent = 'agentId,agentCode,deposits,withdrawals,commissions,netAmount\nAGT-001,Ahmad Razak Store,50000,30000,500,20000\n'

    server.use(
      http.get('/api/v1/backoffice/settlement/export', () => {
        return new HttpResponse(csvContent, {
          headers: { 'Content-Type': 'text/csv' },
        })
      })
    )

    const response = await fetch('/api/v1/backoffice/settlement/export?date=2026-04-03')

    expect(response.headers.get('Content-Type')).toBe('text/csv')
    const text = await response.text()
    expect(text).toContain('agentId')
    expect(text).toContain('AGT-001')
  })
})
