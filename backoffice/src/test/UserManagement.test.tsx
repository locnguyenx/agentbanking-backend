import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { UserManagement } from '../pages/UserManagement'

vi.mock('../api/client', () => ({
  default: {
    getUsers: vi.fn().mockResolvedValue([
      { userId: 'u0000000-0000-0000-0000-000000000001', username: 'admin', fullName: 'Admin User', email: 'admin@bank.com', status: 'ACTIVE', userType: 'INTERNAL', createdAt: '2026-01-15T10:00:00Z', lastLoginAt: '2026-04-01T08:30:00Z' },
      { userId: 'u0000000-0000-0000-0000-000000000002', username: 'teller01', fullName: 'Ahmad Teller', email: 'ahmad@bank.com', status: 'ACTIVE', userType: 'INTERNAL', createdAt: '2026-02-01T09:00:00Z', lastLoginAt: '2026-04-01T09:00:00Z' },
      { userId: 'u0000000-0000-0000-0000-000000000003', username: 'AGT-001', fullName: 'Agent One', email: 'agent1@bank.com', status: 'ACTIVE', userType: 'EXTERNAL', agentId: 'a0000000-0000-0000-0000-000000000001', createdAt: '2026-02-15T10:00:00Z', lastLoginAt: null },
      { userId: 'u0000000-0000-0000-0000-000000000004', username: 'lockeduser', fullName: 'Locked User', email: 'locked@bank.com', status: 'LOCKED', userType: 'INTERNAL', createdAt: '2026-03-01T10:00:00Z', lastLoginAt: '2026-03-28T14:00:00Z' },
      { userId: 'u0000000-0000-0000-0000-000000000005', username: 'disableduser', fullName: 'Disabled User', email: 'disabled@bank.com', status: 'DISABLED', userType: 'INTERNAL', createdAt: '2026-01-20T10:00:00Z', lastLoginAt: '2026-03-15T10:00:00Z' },
      { userId: 'u0000000-0000-0000-0000-000000000006', username: 'teller02', fullName: 'Siti Teller', email: 'siti@bank.com', status: 'ACTIVE', userType: 'INTERNAL', createdAt: '2026-03-10T10:00:00Z', lastLoginAt: '2026-04-01T07:00:00Z' },
      { userId: 'u0000000-0000-0000-0000-000000000007', username: 'AGT-002', fullName: 'Agent Two', email: 'agent2@bank.com', status: 'ACTIVE', userType: 'EXTERNAL', agentId: 'a0000000-0000-0000-0000-000000000002', createdAt: '2026-03-15T10:00:00Z', lastLoginAt: null },
    ]),
    createUser: vi.fn().mockResolvedValue({}),
    updateUser: vi.fn().mockResolvedValue({}),
    deleteUser: vi.fn().mockResolvedValue({}),
    lockUser: vi.fn().mockResolvedValue({}),
    unlockUser: vi.fn().mockResolvedValue({}),
    resetUserPassword: vi.fn().mockResolvedValue({}),
  },
}))

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false },
  },
})

const renderWithQuery = (ui: React.ReactElement) => {
  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
    </MemoryRouter>
  )
}

describe('UserManagement', () => {
  it('should render user management page', async () => {
    renderWithQuery(<UserManagement />)
    
    await waitFor(() => {
      expect(screen.getByText('User Management')).toBeInTheDocument()
    })
  })

  it('should render Add User button', async () => {
    renderWithQuery(<UserManagement />)
    
    await waitFor(() => {
      expect(screen.getByTestId('add-user-button')).toBeInTheDocument()
    })
  })

  it('should render users table with data', async () => {
    renderWithQuery(<UserManagement />)
    
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument()
      expect(screen.getByText('teller01')).toBeInTheDocument()
    })
  })

  it('should open Add User modal when button is clicked', async () => {
    renderWithQuery(<UserManagement />)
    
    await waitFor(() => {
      const addButton = screen.getByTestId('add-user-button')
      fireEvent.click(addButton)
    })
    
    await waitFor(() => {
      expect(screen.getByTestId('add-user-modal')).toBeInTheDocument()
      expect(screen.getByText('Add New User')).toBeInTheDocument()
    })
  })

  it('should close modal when close button is clicked', async () => {
    renderWithQuery(<UserManagement />)
    
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
    renderWithQuery(<UserManagement />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-1-button')).toBeInTheDocument()
      expect(screen.getByTestId('prev-page-button')).toBeInTheDocument()
      expect(screen.getByTestId('next-page-button')).toBeInTheDocument()
    })
  })

  it('should filter users by status', async () => {
    renderWithQuery(<UserManagement />)
    
    await waitFor(() => {
      const statusFilter = screen.getByTestId('status-filter')
      fireEvent.change(statusFilter, { target: { value: 'ACTIVE' } })
    })
    
    await waitFor(() => {
      // Should still show active users
      expect(screen.getByText('admin')).toBeInTheDocument()
    })
  })

  it('should search users by name', async () => {
    renderWithQuery(<UserManagement />)
    
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
    renderWithQuery(<UserManagement />)
    
    await waitFor(() => {
      const activeBadges = screen.getAllByText('ACTIVE')
      expect(activeBadges.length).toBeGreaterThan(0)
    })
  })

  it('should display user type badges', async () => {
    renderWithQuery(<UserManagement />)
    
    await waitFor(() => {
      const internalBadges = screen.getAllByText('INTERNAL')
      expect(internalBadges.length).toBeGreaterThan(0)
    })
  })

  it('should show action menu when action button is clicked', async () => {
    renderWithQuery(<UserManagement />)
    
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
    renderWithQuery(<UserManagement />)
    
    await waitFor(() => {
      const prevButton = screen.getByTestId('prev-page-button')
      expect(prevButton).toBeDisabled()
    })
  })

  it('should enable prev button after navigating to next page', async () => {
    renderWithQuery(<UserManagement />)
    
    await waitFor(() => {
      fireEvent.click(screen.getByTestId('next-page-button'))
    })
    
    await waitFor(() => {
      const prevButton = screen.getByTestId('prev-page-button')
      expect(prevButton).not.toBeDisabled()
    })
  })
})
