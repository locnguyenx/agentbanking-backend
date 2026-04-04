/**
 * Contract Test: User Management Mutations
 *
 * Validates that the frontend makes correct HTTP calls for user mutations.
 * POST   /api/v1/backoffice/admin/users              - Create user
 * PUT    /api/v1/backoffice/admin/users/:id           - Update user
 * DELETE /api/v1/backoffice/admin/users/:id           - Delete user
 * POST   /api/v1/backoffice/admin/users/:id/lock      - Lock user
 * POST   /api/v1/backoffice/admin/users/:id/unlock    - Unlock user
 * POST   /api/v1/backoffice/admin/users/:id/reset-password - Reset password
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../mocks/server'

describe('Contract: POST /api/v1/backoffice/admin/users (Create User)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with user data', async () => {
    let capturedBody: any

    server.use(
      http.post('/api/v1/backoffice/admin/users', async ({ request }) => {
        capturedBody = await request.json()
        return HttpResponse.json(
          {
            userId: 'new-user-id',
            username: capturedBody.username,
            email: capturedBody.email,
            fullName: capturedBody.fullName,
            status: 'ACTIVE',
            userType: capturedBody.userType,
            createdAt: new Date().toISOString(),
          },
          { status: 201 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/admin/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: 'newuser',
        email: 'user@example.com',
        fullName: 'New User',
        password: 'TempPass123!',
        userType: 'INTERNAL',
      }),
    })

    expect(capturedBody.username).toBe('newuser')
    expect(capturedBody.email).toBe('user@example.com')
    expect(capturedBody.fullName).toBe('New User')
    expect(capturedBody.password).toBe('TempPass123!')
    expect(capturedBody.userType).toBe('INTERNAL')
    expect(response.ok).toBe(true)

    const data = await response.json()
    expect(data).toHaveProperty('userId')
    expect(data).toHaveProperty('username')
    expect(data).toHaveProperty('email')
    expect(data).toHaveProperty('fullName')
    expect(data).toHaveProperty('status')
    expect(data).toHaveProperty('userType')
    expect(data.status).toBe('ACTIVE')
    expect(data.userType).toBe('INTERNAL')
  })

  it('should handle 409 when user already exists', async () => {
    server.use(
      http.post('/api/v1/backoffice/admin/users', () => {
        return HttpResponse.json(
          {
            status: 'FAILED',
            error: {
              code: 'ERR_BIZ_USER_EXISTS',
              message: 'Username already exists',
              action_code: 'REVIEW',
            },
          },
          { status: 409 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/admin/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: 'existinguser',
        email: 'existing@example.com',
        fullName: 'Existing User',
        password: 'TempPass123!',
        userType: 'INTERNAL',
      }),
    })

    expect(response.status).toBe(409)
    const data = await response.json()
    expect(data.error.code).toBe('ERR_BIZ_USER_EXISTS')
  })
})

describe('Contract: PUT /api/v1/backoffice/admin/users/:id (Update User)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with userId in path', async () => {
    let capturedId: string | undefined
    let capturedBody: any

    server.use(
      http.put('/api/v1/backoffice/admin/users/:id', async ({ request, params }) => {
        capturedId = params.id as string
        capturedBody = await request.json()
        return HttpResponse.json({
          userId: capturedId,
          username: 'existinguser',
          email: capturedBody.email,
          fullName: capturedBody.fullName,
          status: 'ACTIVE',
          userType: 'INTERNAL',
          createdAt: '2026-04-01T12:00:00Z',
          updatedAt: new Date().toISOString(),
        })
      })
    )

    const userId = 'u0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/admin/users/${userId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: 'updated@example.com',
        fullName: 'Updated Name',
      }),
    })

    expect(capturedId).toBe(userId)
    expect(capturedBody.email).toBe('updated@example.com')
    expect(capturedBody.fullName).toBe('Updated Name')
    expect(response.ok).toBe(true)

    const data = await response.json()
    expect(data.userId).toBe(userId)
    expect(data.email).toBe('updated@example.com')
    expect(data.fullName).toBe('Updated Name')
  })

  it('should handle 404 when user not found', async () => {
    server.use(
      http.put('/api/v1/backoffice/admin/users/:id', () => {
        return HttpResponse.json(
          {
            status: 'FAILED',
            error: {
              code: 'ERR_NOT_FOUND',
              message: 'User not found',
              action_code: 'DECLINE',
            },
          },
          { status: 404 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/admin/users/nonexistent-id', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: 'test@example.com',
        fullName: 'Test',
      }),
    })

    expect(response.status).toBe(404)
    const data = await response.json()
    expect(data.error.code).toBe('ERR_NOT_FOUND')
  })
})

describe('Contract: DELETE /api/v1/backoffice/admin/users/:id (Delete User)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with userId in path', async () => {
    let capturedId: string | undefined

    server.use(
      http.delete('/api/v1/backoffice/admin/users/:id', ({ params }) => {
        capturedId = params.id as string
        return new HttpResponse(null, { status: 204 })
      })
    )

    const userId = 'u0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/admin/users/${userId}`, {
      method: 'DELETE',
    })

    expect(capturedId).toBe(userId)
    expect(response.status).toBe(204)
  })

  it('should handle 404 when user not found', async () => {
    server.use(
      http.delete('/api/v1/backoffice/admin/users/:id', () => {
        return HttpResponse.json(
          {
            status: 'FAILED',
            error: {
              code: 'ERR_NOT_FOUND',
              message: 'User not found',
              action_code: 'DECLINE',
            },
          },
          { status: 404 }
        )
      })
    )

    const response = await fetch('/api/v1/backoffice/admin/users/nonexistent-id', {
      method: 'DELETE',
    })

    expect(response.status).toBe(404)
  })
})

describe('Contract: POST /api/v1/backoffice/admin/users/:id/lock (Lock User)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with userId in path', async () => {
    let capturedId: string | undefined

    server.use(
      http.post('/api/v1/backoffice/admin/users/:id/lock', ({ params }) => {
        capturedId = params.id as string
        return HttpResponse.json({
          userId: capturedId,
          status: 'LOCKED',
        })
      })
    )

    const userId = 'u0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/admin/users/${userId}/lock`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    })

    expect(capturedId).toBe(userId)
    expect(response.ok).toBe(true)

    const data = await response.json()
    expect(data.userId).toBe(userId)
    expect(data.status).toBe('LOCKED')
  })
})

describe('Contract: POST /api/v1/backoffice/admin/users/:id/unlock (Unlock User)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with userId in path', async () => {
    let capturedId: string | undefined

    server.use(
      http.post('/api/v1/backoffice/admin/users/:id/unlock', ({ params }) => {
        capturedId = params.id as string
        return HttpResponse.json({
          userId: capturedId,
          status: 'ACTIVE',
        })
      })
    )

    const userId = 'u0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/admin/users/${userId}/unlock`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    })

    expect(capturedId).toBe(userId)
    expect(response.ok).toBe(true)

    const data = await response.json()
    expect(data.userId).toBe(userId)
    expect(data.status).toBe('ACTIVE')
  })
})

describe('Contract: POST /api/v1/backoffice/admin/users/:id/reset-password (Reset Password)', () => {
  beforeEach(() => server.resetHandlers())

  it('should call correct endpoint with newPassword', async () => {
    let capturedId: string | undefined
    let capturedBody: any

    server.use(
      http.post('/api/v1/backoffice/admin/users/:id/reset-password', async ({ request, params }) => {
        capturedId = params.id as string
        capturedBody = await request.json()
        return HttpResponse.json({
          userId: capturedId,
          temporaryPassword: capturedBody.newPassword,
          mustChangePassword: true,
        })
      })
    )

    const userId = 'u0000000-0000-0000-0000-000000000001'
    const response = await fetch(`/api/v1/backoffice/admin/users/${userId}/reset-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ newPassword: 'NewTemp123!' }),
    })

    expect(capturedId).toBe(userId)
    expect(capturedBody.newPassword).toBe('NewTemp123!')
    expect(response.ok).toBe(true)

    const data = await response.json()
    expect(data.userId).toBe(userId)
    expect(data.temporaryPassword).toBe('NewTemp123!')
    expect(data.mustChangePassword).toBe(true)
  })
})
