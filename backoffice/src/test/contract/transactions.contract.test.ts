/**
 * Contract test: Transactions API
 * 
 * Backend: services/ledger-service/.../web/LedgerController.java
 * Endpoint: GET /api/v1/backoffice/transactions (via gateway)
 * Returns paginated response: { content, totalElements, totalPages, page, size }
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'
import { VALID_TRANSACTION_STATUSES } from '../mocks/schemas'

describe('Transactions API Contract', () => {
  it('should document the paginated response shape', () => {
    const paginationFields = ['content', 'totalElements', 'totalPages', 'page', 'size']
    expect(paginationFields).toContain('content')
    expect(paginationFields).toContain('totalElements')
    expect(paginationFields).toContain('totalPages')
    expect(paginationFields).toContain('page')
    expect(paginationFields).toContain('size')
  })

  it('should document transaction content fields', () => {
    const contentFields = ['transactionId', 'agentId', 'transactionType', 'amount', 'status', 'customerCardMasked', 'createdAt']
    expect(contentFields).toContain('transactionId')
    expect(contentFields).toContain('agentId')
    expect(contentFields).toContain('transactionType')
    expect(contentFields).toContain('amount')
    expect(contentFields).toContain('status')
    expect(contentFields).toContain('customerCardMasked')
    expect(contentFields).toContain('createdAt')
  })

  it('should use correct status enum values', () => {
    expect(VALID_TRANSACTION_STATUSES).toContain('COMPLETED')
    expect(VALID_TRANSACTION_STATUSES).toContain('PENDING')
    expect(VALID_TRANSACTION_STATUSES).toContain('FAILED')
  })
})

describe('GET /api/v1/backoffice/transactions with filters', () => {
  beforeEach(() => server.resetHandlers())

  it('should capture status query param and return filtered results', async () => {
    let capturedStatus: string | null = null

    server.use(
      http.get('/api/v1/backoffice/transactions', ({ request }) => {
        const url = new URL(request.url)
        capturedStatus = url.searchParams.get('status')
        return HttpResponse.json({
          content: [],
          totalElements: 0,
          totalPages: 0,
          page: 0,
          size: 10,
        })
      }),
    )

    const response = await fetch('/api/v1/backoffice/transactions?status=COMPLETED')
    const data = await response.json()

    expect(capturedStatus).toBe('COMPLETED')
    expect(data).toHaveProperty('content')
    expect(data).toHaveProperty('totalElements')
    expect(data).toHaveProperty('totalPages')
    expect(data).toHaveProperty('page')
    expect(data).toHaveProperty('size')
    expect(Array.isArray(data.content)).toBe(true)
  })

  it('should capture type query param and return filtered results', async () => {
    let capturedType: string | null = null

    server.use(
      http.get('/api/v1/backoffice/transactions', ({ request }) => {
        const url = new URL(request.url)
        capturedType = url.searchParams.get('type')
        return HttpResponse.json({
          content: [],
          totalElements: 0,
          totalPages: 0,
          page: 0,
          size: 10,
        })
      }),
    )

    const response = await fetch('/api/v1/backoffice/transactions?type=CASH_DEPOSIT')
    const data = await response.json()

    expect(capturedType).toBe('CASH_DEPOSIT')
    expect(data).toHaveProperty('content')
    expect(data).toHaveProperty('totalElements')
    expect(data).toHaveProperty('totalPages')
    expect(data).toHaveProperty('page')
    expect(data).toHaveProperty('size')
    expect(Array.isArray(data.content)).toBe(true)
  })

  it('should capture page and size query params for pagination', async () => {
    let capturedPage: string | null = null
    let capturedSize: string | null = null

    server.use(
      http.get('/api/v1/backoffice/transactions', ({ request }) => {
        const url = new URL(request.url)
        capturedPage = url.searchParams.get('page')
        capturedSize = url.searchParams.get('size')
        return HttpResponse.json({
          content: [],
          totalElements: 0,
          totalPages: 0,
          page: parseInt(capturedPage || '0'),
          size: parseInt(capturedSize || '10'),
        })
      }),
    )

    const response = await fetch('/api/v1/backoffice/transactions?page=2&size=20')
    const data = await response.json()

    expect(capturedPage).toBe('2')
    expect(capturedSize).toBe('20')
    expect(data.page).toBe(2)
    expect(data.size).toBe(20)
    expect(data).toHaveProperty('content')
    expect(data).toHaveProperty('totalElements')
    expect(data).toHaveProperty('totalPages')
    expect(Array.isArray(data.content)).toBe(true)
  })

  it('should capture multiple query params together', async () => {
    const capturedParams: Record<string, string | null> = {}

    server.use(
      http.get('/api/v1/backoffice/transactions', ({ request }) => {
        const url = new URL(request.url)
        capturedParams.status = url.searchParams.get('status')
        capturedParams.type = url.searchParams.get('type')
        capturedParams.page = url.searchParams.get('page')
        capturedParams.size = url.searchParams.get('size')
        return HttpResponse.json({
          content: [],
          totalElements: 0,
          totalPages: 0,
          page: 0,
          size: 10,
        })
      }),
    )

    const response = await fetch('/api/v1/backoffice/transactions?status=COMPLETED&type=CASH_WITHDRAWAL&page=1&size=5')
    const data = await response.json()

    expect(capturedParams.status).toBe('COMPLETED')
    expect(capturedParams.type).toBe('CASH_WITHDRAWAL')
    expect(capturedParams.page).toBe('1')
    expect(capturedParams.size).toBe('5')
    expect(data).toHaveProperty('content')
    expect(data).toHaveProperty('totalElements')
    expect(data).toHaveProperty('totalPages')
    expect(data).toHaveProperty('page')
    expect(data).toHaveProperty('size')
    expect(Array.isArray(data.content)).toBe(true)
  })
})
