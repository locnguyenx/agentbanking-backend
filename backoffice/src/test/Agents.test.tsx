import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { Agents } from '../pages/Agents'
import React from 'react'

vi.mock('../api/client', () => {
  const MOCK_AGENTS = Array.from({ length: 12 }, (_, i) => ({
    agentId: `a0000000-0000-0000-0000-${String(i + 1).padStart(12, '0')}`,
    agentCode: `AGT-${String(i + 1).padStart(3, '0')}`,
    businessName: `Agent Store ${i + 1}`,
    phoneNumber: `012-${String(3456789 + i).slice(-7)}`,
    status: i % 3 === 0 ? 'SUSPENDED' : 'ACTIVE',
    tier: i < 4 ? 'PREMIUM' : i < 8 ? 'STANDARD' : 'BASIC',
    merchantGpsLat: 3.139003 + (i * 0.01),
    merchantGpsLng: 101.686855 + (i * 0.01),
    createdAt: '2026-04-01T12:56:49.938566',
    updatedAt: '2026-04-01T12:56:49.938566',
  }))

  return {
    default: {
      getDashboard: vi.fn().mockResolvedValue({}),
      getAgents: vi.fn().mockResolvedValue(MOCK_AGENTS),
      createAgent: vi.fn().mockResolvedValue({}),
      getAgent: vi.fn().mockResolvedValue({}),
      updateAgent: vi.fn().mockResolvedValue({}),
      deactivateAgent: vi.fn().mockResolvedValue({}),
      getUsers: vi.fn().mockResolvedValue([]),
      createUser: vi.fn().mockResolvedValue({}),
      updateUser: vi.fn().mockResolvedValue({}),
      deleteUser: vi.fn().mockResolvedValue({}),
      lockUser: vi.fn().mockResolvedValue({}),
      unlockUser: vi.fn().mockResolvedValue({}),
      resetUserPassword: vi.fn().mockResolvedValue({}),
      getAgentUserStatus: vi.fn().mockResolvedValue({ status: 'NOT_CREATED' }),
      createAgentUser: vi.fn().mockResolvedValue({}),
      getAgentFloat: vi.fn().mockResolvedValue({ exists: false }),
      getAgentFloatTransactions: vi.fn().mockResolvedValue({ agentId: '', period: '2026-04', totalCount: 0, totalVolume: 0, byType: [] }),
      createAgentFloat: vi.fn().mockResolvedValue({ exists: true, float: { balance: 5000 } }),
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

describe('Agents', () => {
  it('should render agents page', async () => {
    renderWithProviders(<Agents />)
    
    await waitFor(() => {
      expect(screen.getByText('Agent Management')).toBeInTheDocument()
    })
  })

  it('should render Add Agent button', async () => {
    renderWithProviders(<Agents />)
    
    await waitFor(() => {
      expect(screen.getByTestId('add-agent-button')).toBeInTheDocument()
    })
  })

  it('should open Add Agent modal when button is clicked', async () => {
    renderWithProviders(<Agents />)
    
    await waitFor(() => {
      fireEvent.click(screen.getByTestId('add-agent-button'))
    })
    
    await waitFor(() => {
      expect(screen.getByTestId('add-agent-modal')).toBeInTheDocument()
      expect(screen.getByText('Add New Agent')).toBeInTheDocument()
    })
  })

  it('should close modal when close button is clicked', async () => {
    renderWithProviders(<Agents />)
    
    await waitFor(() => {
      fireEvent.click(screen.getByTestId('add-agent-button'))
    })
    
    await waitFor(() => {
      expect(screen.getByTestId('add-agent-modal')).toBeInTheDocument()
    })
    
    await waitFor(() => {
      fireEvent.click(screen.getByTestId('close-modal-button'))
    })
    
    await waitFor(() => {
      expect(screen.queryByTestId('add-agent-modal')).not.toBeInTheDocument()
    })
  })

  it('should render pagination buttons', async () => {
    renderWithProviders(<Agents />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-1-button')).toBeInTheDocument()
      expect(screen.getByTestId('page-2-button')).toBeInTheDocument()
      expect(screen.getByTestId('prev-page-button')).toBeInTheDocument()
      expect(screen.getByTestId('next-page-button')).toBeInTheDocument()
    })
  })

  it('should change page when page number is clicked', async () => {
    renderWithProviders(<Agents />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-1-button')).toBeInTheDocument()
      expect(screen.getByTestId('page-2-button')).toBeInTheDocument()
    })
    
    fireEvent.click(screen.getByTestId('page-2-button'))
    
    await waitFor(() => {
      expect(screen.getByText(/Showing \d+ to \d+ of 12 agents/)).toBeInTheDocument()
    })
  })

  it('should enable next page button when on first page', async () => {
    renderWithProviders(<Agents />)
    
    await waitFor(() => {
      const prevButton = screen.getByTestId('prev-page-button')
      expect(prevButton).toBeDisabled()
    })
  })

  it('should display Float Balance column in table', async () => {
    renderWithProviders(<Agents />)
    
    await waitFor(() => {
      expect(screen.getByText('Float Balance')).toBeInTheDocument()
    })
  })

  it('should show No Float for agents without float', async () => {
    renderWithProviders(<Agents />)
    
    await waitFor(() => {
      expect(screen.getAllByText('No Float').length).toBeGreaterThan(0)
    })
  })

  it('should display Create AgentFloat option in action menu', async () => {
    renderWithProviders(<Agents />)
    
    await waitFor(() => {
      const firstActionButton = screen.getByTestId('action-a0000000-0000-0000-0000-000000000001-button')
      fireEvent.click(firstActionButton)
    })
    
    await waitFor(() => {
      expect(screen.getByText('Create AgentFloat')).toBeInTheDocument()
    })
  })

  it('should open Create AgentFloat modal when action is clicked', async () => {
    renderWithProviders(<Agents />)
    
    await waitFor(() => {
      const firstActionButton = screen.getByTestId('action-a0000000-0000-0000-0000-000000000001-button')
      fireEvent.click(firstActionButton)
    })
    
    await waitFor(() => {
      fireEvent.click(screen.getByText('Create AgentFloat'))
    })
    
    await waitFor(() => {
      expect(screen.getByText('Create Agent Float')).toBeInTheDocument()
      expect(screen.getByText('Initial Balance')).toBeInTheDocument()
      expect(screen.getByText('Currency')).toBeInTheDocument()
    })
  })
})
