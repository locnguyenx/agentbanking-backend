import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { UserManagement } from '../pages/UserManagement'
import React from 'react'

vi.mock('../api/client', () => {
  const MOCK_USERS = [
    { userId: 'u0000000-0000-0000-0000-000000000001', username: 'admin', email: 'admin@agentbanking.com', fullName: 'System Administrator', status: 'ACTIVE', userType: 'INTERNAL', agentId: null, permissions: ['user:read', 'user:write'], createdAt: '2026-01-15T10:00:00Z', lastLoginAt: '2026-04-01T08:30:00Z' },
    { userId: 'u0000000-0000-0000-0000-000000000002', username: 'teller01', email: 'teller01@bank.com', fullName: 'Ahmad Teller', status: 'ACTIVE', userType: 'INTERNAL', agentId: null, permissions: [], createdAt: '2026-02-01T09:00:00Z', lastLoginAt: '2026-04-01T09:00:00Z' },
    { userId: 'u0000000-0000-0000-0000-000000000003', username: 'operator01', email: 'operator01@bank.com', fullName: 'Siti Operator', status: 'ACTIVE', userType: 'INTERNAL', agentId: null, permissions: [], createdAt: '2026-02-15T10:00:00Z', lastLoginAt: '2026-04-01T07:00:00Z' },
    { userId: 'u0000000-0000-0000-0000-000000000004', username: 'maker01', email: 'maker01@bank.com', fullName: 'Fatimah Maker', status: 'ACTIVE', userType: 'INTERNAL', agentId: null, permissions: [], createdAt: '2026-03-01T10:00:00Z', lastLoginAt: null },
    { userId: 'u0000000-0000-0000-0000-000000000005', username: 'checker01', email: 'checker01@bank.com', fullName: 'Ali Checker', status: 'ACTIVE', userType: 'INTERNAL', agentId: null, permissions: [], createdAt: '2026-03-10T10:00:00Z', lastLoginAt: null },
    { userId: 'u0000000-0000-0000-0000-000000000006', username: 'lockeduser', email: 'locked@bank.com', fullName: 'Locked User', status: 'LOCKED', userType: 'INTERNAL', agentId: null, permissions: [], createdAt: '2026-03-15T10:00:00Z', lastLoginAt: '2026-03-28T14:00:00Z' },
    { userId: 'u0000000-0000-0000-0000-000000000007', username: 'inactiveuser', email: 'inactive@bank.com', fullName: 'Inactive User', status: 'INACTIVE', userType: 'INTERNAL', agentId: null, permissions: [], createdAt: '2026-01-20T10:00:00Z', lastLoginAt: null },
    { userId: 'u0000000-0000-0000-0000-000000000008', username: 'deleteduser', email: 'deleted@bank.com', fullName: 'Deleted User', status: 'DELETED', userType: 'INTERNAL', agentId: null, permissions: [], createdAt: '2026-01-10T10:00:00Z', lastLoginAt: null },
    { userId: 'u0000000-0000-0000-0000-000000000009', username: 'supervisor01', email: 'supervisor01@bank.com', fullName: 'Abu Supervisor', status: 'ACTIVE', userType: 'INTERNAL', agentId: null, permissions: [], createdAt: '2026-02-20T10:00:00Z', lastLoginAt: '2026-04-01T06:00:00Z' },
    { userId: 'u0000000-0000-0000-0000-000000000010', username: 'compliance01', email: 'compliance01@bank.com', fullName: 'Nurul Compliance', status: 'ACTIVE', userType: 'INTERNAL', agentId: null, permissions: [], createdAt: '2026-03-05T10:00:00Z', lastLoginAt: '2026-03-30T10:00:00Z' },
  ]

  return {
    default: {
      getDashboard: vi.fn().mockResolvedValue({}),
      getAgents: vi.fn().mockResolvedValue([]),
      createAgent: vi.fn().mockResolvedValue({}),
      getAgent: vi.fn().mockResolvedValue({}),
      updateAgent: vi.fn().mockResolvedValue({}),
      deactivateAgent: vi.fn().mockResolvedValue({}),
      getUsers: vi.fn().mockResolvedValue(MOCK_USERS),
      createUser: vi.fn().mockResolvedValue({}),
      updateUser: vi.fn().mockResolvedValue({}),
      deleteUser: vi.fn().mockResolvedValue({}),
      lockUser: vi.fn().mockResolvedValue({}),
      unlockUser: vi.fn().mockResolvedValue({}),
      resetUserPassword: vi.fn().mockResolvedValue({}),
      getAgentUserStatus: vi.fn().mockResolvedValue({}),
      createAgentUser: vi.fn().mockResolvedValue({}),
      getTransactions: vi.fn().mockResolvedValue({ content: [], totalElements: 0 }),
      getSettlement: vi.fn().mockResolvedValue({}),
      exportSettlement: vi.fn().mockResolvedValue({}),
      getKycReviewQueue: vi.fn().mockResolvedValue({}),
      approveKyc: vi.fn().mockResolvedValue({}),
      rejectKyc: vi.fn().mockResolvedValue({}),
      login: vi.fn().mockResolvedValue({}),
      logout: vi.fn(),
    },
  }
})

function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
    },
  })
}

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = createTestQueryClient()
  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
    </MemoryRouter>
  )
}

describe('UserManagement', () => {
  it('should render user management page', async () => {
    renderWithProviders(<UserManagement />)
    
    await waitFor(() => {
      expect(screen.getByText('User Management')).toBeInTheDocument()
    })
  })

  it('should render Add User button', async () => {
    renderWithProviders(<UserManagement />)
    
    await waitFor(() => {
      expect(screen.getByTestId('add-user-button')).toBeInTheDocument()
    })
  })

  it('should render users table with data', async () => {
    renderWithProviders(<UserManagement />)
    
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
      expect(screen.getByText('teller01')).toBeInTheDocument()
    })
  })

  it('should open Add User modal when button is clicked', async () => {
    renderWithProviders(<UserManagement />)
    
    await waitFor(() => {
      fireEvent.click(screen.getByTestId('add-user-button'))
    })
    
    await waitFor(() => {
      expect(screen.getByTestId('add-user-modal')).toBeInTheDocument()
      expect(screen.getByText('Add New User')).toBeInTheDocument()
    })
  })

  it('should close modal when close button is clicked', async () => {
    renderWithProviders(<UserManagement />)
    
    await waitFor(() => {
      fireEvent.click(screen.getByTestId('add-user-button'))
    })
    
    await waitFor(() => {
      expect(screen.getByTestId('add-user-modal')).toBeInTheDocument()
    })
    
    await waitFor(() => {
      fireEvent.click(screen.getByTestId('close-modal-button'))
    })
    
    await waitFor(() => {
      expect(screen.queryByTestId('add-user-modal')).not.toBeInTheDocument()
    })
  })

  it('should render pagination buttons', async () => {
    renderWithProviders(<UserManagement />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-1-button')).toBeInTheDocument()
      expect(screen.getByTestId('page-2-button')).toBeInTheDocument()
      expect(screen.getByTestId('prev-page-button')).toBeInTheDocument()
      expect(screen.getByTestId('next-page-button')).toBeInTheDocument()
    })
  })

  it('should filter users by status', async () => {
    renderWithProviders(<UserManagement />)
    
    await waitFor(() => {
      const statusFilter = screen.getByTestId('status-filter')
      fireEvent.change(statusFilter, { target: { value: 'ACTIVE' } })
    })
    
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
    })
  })

  it('should search users by name', async () => {
    renderWithProviders(<UserManagement />)
    
    await waitFor(() => {
      const searchInput = screen.getByTestId('search-input')
      fireEvent.change(searchInput, { target: { value: 'Admin' } })
    })
    
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
      expect(screen.queryByText('teller01')).not.toBeInTheDocument()
    })
  })

  it('should display user status badges', async () => {
    renderWithProviders(<UserManagement />)
    
    await waitFor(() => {
      const activeBadges = screen.getAllByText('ACTIVE')
      expect(activeBadges.length).toBeGreaterThan(0)
    })
  })

  it('should show action menu when action button is clicked', async () => {
    renderWithProviders(<UserManagement />)
    
    await waitFor(() => {
      const firstUserRow = screen.getByText('admin').closest('tr')
      const actionButton = firstUserRow?.querySelector('button[data-testid^="action-"]')
      if (actionButton) {
        fireEvent.click(actionButton)
      }
    })
    
    await waitFor(() => {
      expect(screen.getByText('View Details')).toBeInTheDocument()
      expect(screen.getByText('Edit User')).toBeInTheDocument()
      expect(screen.getByText('Reset Password')).toBeInTheDocument()
    })
  })

  it('should disable prev button on first page', async () => {
    renderWithProviders(<UserManagement />)
    
    await waitFor(() => {
      const prevButton = screen.getByTestId('prev-page-button')
      expect(prevButton).toBeDisabled()
    })
  })

  it('should enable prev button after navigating to next page', async () => {
    renderWithProviders(<UserManagement />)
    
    await waitFor(() => {
      fireEvent.click(screen.getByTestId('next-page-button'))
    })
    
    await waitFor(() => {
      const prevButton = screen.getByTestId('prev-page-button')
      expect(prevButton).not.toBeDisabled()
    })
  })
})
