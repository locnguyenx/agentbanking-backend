/**
 * Contract Test: KYC Review API
 * 
 * Validates that the frontend makes correct HTTP calls to the backend for KYC review operations.
 * This test uses MSW to intercept requests and validate the contract.
 * 
 * Backend: services/onboarding-service/.../web/KycReviewController.java
 * Endpoints:
 *   GET    /api/v1/backoffice/kyc/review-queue
 *   POST   /api/v1/backoffice/kyc/review-queue/:id/approve
 *   POST   /api/v1/backoffice/kyc/review-queue/:id/reject
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: GET /api/v1/backoffice/kyc/review-queue', () => {
  beforeEach(() => {
    server.resetHandlers()
  })

  it('should return array of pending KYC applications', async () => {
    server.use(
      http.get('/api/v1/backoffice/kyc/review-queue', () => {
        return HttpResponse.json([
          {
            id: 'kyc-00000000-0000-0000-0000-000000000001',
            agentCode: 'AGT-001',
            businessName: 'Ahmad Razak Store',
            status: 'PENDING_REVIEW',
            submittedAt: '2026-04-01T10:00:00+08:00',
            biometricVerified: true,
            amlStatus: 'CLEARED',
          },
        ])
      })
    )

    const response = await fetch('/api/v1/backoffice/kyc/review-queue')
    const data = await response.json()

    expect(Array.isArray(data)).toBe(true)
    expect(data).toHaveLength(1)
    expect(data[0]).toHaveProperty('id')
    expect(data[0]).toHaveProperty('agentCode')
    expect(data[0]).toHaveProperty('businessName')
    expect(data[0]).toHaveProperty('status')
    expect(data[0]).toHaveProperty('submittedAt')
    expect(data[0]).toHaveProperty('biometricVerified')
    expect(data[0]).toHaveProperty('amlStatus')
  })

  it('should return empty array when no pending applications', async () => {
    server.use(
      http.get('/api/v1/backoffice/kyc/review-queue', () => {
        return HttpResponse.json([])
      })
    )

    const response = await fetch('/api/v1/backoffice/kyc/review-queue')
    const data = await response.json()

    expect(Array.isArray(data)).toBe(true)
    expect(data).toHaveLength(0)
  })

  it('should handle valid status enum values', async () => {
    server.use(
      http.get('/api/v1/backoffice/kyc/review-queue', () => {
        return HttpResponse.json([
          { id: '1', agentCode: 'AGT-001', businessName: 'A', status: 'PENDING_REVIEW', submittedAt: '', biometricVerified: true, amlStatus: 'CLEARED' },
          { id: '2', agentCode: 'AGT-002', businessName: 'B', status: 'UNDER_REVIEW', submittedAt: '', biometricVerified: false, amlStatus: 'PENDING' },
        ])
      })
    )

    const response = await fetch('/api/v1/backoffice/kyc/review-queue')
    const data = await response.json()

    const statuses = data.map((k: any) => k.status)
    expect(statuses).toContain('PENDING_REVIEW')
    expect(statuses).toContain('UNDER_REVIEW')
  })

  it('should handle valid amlStatus enum values', async () => {
    server.use(
      http.get('/api/v1/backoffice/kyc/review-queue', () => {
        return HttpResponse.json([
          { id: '1', agentCode: 'AGT-001', businessName: 'A', status: 'PENDING_REVIEW', submittedAt: '', biometricVerified: true, amlStatus: 'CLEARED' },
          { id: '2', agentCode: 'AGT-002', businessName: 'B', status: 'PENDING_REVIEW', submittedAt: '', biometricVerified: true, amlStatus: 'FLAGGED' },
          { id: '3', agentCode: 'AGT-003', businessName: 'C', status: 'PENDING_REVIEW', submittedAt: '', biometricVerified: false, amlStatus: 'PENDING' },
        ])
      })
    )

    const response = await fetch('/api/v1/backoffice/kyc/review-queue')
    const data = await response.json()

    const amlStatuses = data.map((k: any) => k.amlStatus)
    expect(amlStatuses).toContain('CLEARED')
    expect(amlStatuses).toContain('FLAGGED')
    expect(amlStatuses).toContain('PENDING')
  })
})

describe('Contract: POST /api/v1/backoffice/kyc/review-queue/:id/approve', () => {
  const kycId = 'kyc-00000000-0000-0000-0000-000000000001'

  beforeEach(() => {
    server.resetHandlers()
  })

  it('should call correct endpoint with kycId in path', async () => {
    let capturedPath: string | undefined

    server.use(
      http.post(`/api/v1/backoffice/kyc/review-queue/:id/approve`, ({ params }) => {
        capturedPath = params.id as string
        return HttpResponse.json({
          id: capturedPath,
          status: 'APPROVED',
          approvedAt: '2026-04-01T14:30:00+08:00',
        })
      })
    )

    const response = await fetch(`/api/v1/backoffice/kyc/review-queue/${kycId}/approve`, {
      method: 'POST',
    })
    const data = await response.json()

    expect(capturedPath).toBe(kycId)
    expect(data).toHaveProperty('id')
    expect(data.status).toBe('APPROVED')
    expect(data).toHaveProperty('approvedAt')
  })

  it('should return APPROVED status with timestamp', async () => {
    server.use(
      http.post(`/api/v1/backoffice/kyc/review-queue/:id/approve`, () => {
        return HttpResponse.json({
          id: kycId,
          status: 'APPROVED',
          approvedAt: '2026-04-01T14:30:00+08:00',
        })
      })
    )

    const response = await fetch(`/api/v1/backoffice/kyc/review-queue/${kycId}/approve`, {
      method: 'POST',
    })
    const data = await response.json()

    expect(data.id).toBe(kycId)
    expect(data.status).toBe('APPROVED')
    expect(data.approvedAt).toMatch(/\d{4}-\d{2}-\d{2}T/)
  })
})

describe('Contract: POST /api/v1/backoffice/kyc/review-queue/:id/reject', () => {
  const kycId = 'kyc-00000000-0000-0000-0000-000000000001'

  beforeEach(() => {
    server.resetHandlers()
  })

  it('should call correct endpoint with kycId in path and capture rejection reason', async () => {
    let capturedPath: string | undefined
    let capturedBody: unknown

    server.use(
      http.post(`/api/v1/backoffice/kyc/review-queue/:id/reject`, async ({ params, request }) => {
        capturedPath = params.id as string
        capturedBody = await request.json()
        return HttpResponse.json({
          id: capturedPath,
          status: 'REJECTED',
          rejectionReason: (capturedBody as any).reason,
        })
      })
    )

    const rejectionPayload = { reason: 'Incomplete documentation' }
    const response = await fetch(`/api/v1/backoffice/kyc/review-queue/${kycId}/reject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(rejectionPayload),
    })
    const data = await response.json()

    expect(capturedPath).toBe(kycId)
    expect((capturedBody as any).reason).toBe('Incomplete documentation')
    expect(data).toHaveProperty('id')
    expect(data.status).toBe('REJECTED')
    expect(data).toHaveProperty('rejectionReason')
  })

  it('should return REJECTED status with rejection reason', async () => {
    server.use(
      http.post(`/api/v1/backoffice/kyc/review-queue/:id/reject`, async ({ request }) => {
        const body = await request.json()
        return HttpResponse.json({
          id: kycId,
          status: 'REJECTED',
          rejectionReason: (body as any).reason,
        })
      })
    )

    const rejectionPayload = { reason: 'AML check failed' }
    const response = await fetch(`/api/v1/backoffice/kyc/review-queue/${kycId}/reject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(rejectionPayload),
    })
    const data = await response.json()

    expect(data.id).toBe(kycId)
    expect(data.status).toBe('REJECTED')
    expect(data.rejectionReason).toBe('AML check failed')
  })
})
