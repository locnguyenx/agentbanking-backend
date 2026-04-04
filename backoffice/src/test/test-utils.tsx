import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { render, RenderResult } from '@testing-library/react'
import React from 'react'

/**
 * Creates a fresh QueryClient for each test to prevent state leakage.
 * Always use this instead of a shared QueryClient instance.
 */
export function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0, // Immediately garbage collect
      },
    },
  })
}

/**
 * Renders a component with all required providers (QueryClient, Router).
 * Each call creates a fresh QueryClient to prevent state leakage between tests.
 *
 * Usage:
 *   const { screen } = renderWithProviders(<UserManagement />)
 *   const { screen } = renderWithProviders(<Agents />, { route: '/agents?foo=bar' })
 */
export function renderWithProviders(
  ui: React.ReactElement,
  options: { route?: string } = {}
): RenderResult & { queryClient: QueryClient } {
  const queryClient = createTestQueryClient()

  const result = render(
    <MemoryRouter initialEntries={options.route ? [options.route] : undefined}>
      <QueryClientProvider client={queryClient}>
        {ui}
      </QueryClientProvider>
    </MemoryRouter>
  )

  return { ...result, queryClient }
}

/**
 * Creates a type-safe mock for the API client.
 * Usage:
 *   const { mockApi } = createApiMock()
 *   mockApi.getUsers.mockResolvedValue(MOCK_USERS)
 *   vi.mock('../api/client', () => ({ default: mockApi }))
 */
export function createApiMock() {
  return {
    getDashboard: vi.fn(),
    getAgents: vi.fn(),
    createAgent: vi.fn(),
    getAgent: vi.fn(),
    updateAgent: vi.fn(),
    deactivateAgent: vi.fn(),
    getUsers: vi.fn(),
    createUser: vi.fn(),
    updateUser: vi.fn(),
    deleteUser: vi.fn(),
    lockUser: vi.fn(),
    unlockUser: vi.fn(),
    resetUserPassword: vi.fn(),
    getAgentUserStatus: vi.fn(),
    createAgentUser: vi.fn(),
    getTransactions: vi.fn(),
    getSettlement: vi.fn(),
    exportSettlement: vi.fn(),
    getKycReviewQueue: vi.fn(),
    approveKyc: vi.fn(),
    rejectKyc: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
  }
}
