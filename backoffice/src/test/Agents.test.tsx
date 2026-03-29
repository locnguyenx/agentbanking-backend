import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { Agents } from '../pages/Agents'

vi.mock('../api/client', () => ({
  default: {
    getAgents: vi.fn().mockResolvedValue([
      { agentId: 'a0000000-0000-0000-0000-000000000001', agentCode: 'AGT-001', businessName: 'Ahmad Razak Store', phoneNumber: '012-3456789', status: 'ACTIVE', tier: 'PREMIUM', merchantGpsLat: 3.139003, merchantGpsLng: 101.686855 },
      { agentId: 'a0000000-0000-0000-0000-000000000002', agentCode: 'AGT-002', businessName: 'Siti Aminah Store', phoneNumber: '013-8765432', status: 'ACTIVE', tier: 'STANDARD', merchantGpsLat: 3.073400, merchantGpsLng: 101.606500 },
      { agentId: 'a0000000-0000-0000-0000-000000000003', agentCode: 'AGT-003', businessName: 'Faisal Trading', phoneNumber: '014-2468135', status: 'SUSPENDED', tier: 'BASIC', merchantGpsLat: 3.068500, merchantGpsLng: 101.518200 },
      { agentId: 'a0000000-0000-0000-0000-000000000004', agentCode: 'AGT-004', businessName: 'Lee Ming Retail', phoneNumber: '016-9753124', status: 'ACTIVE', tier: 'PREMIUM', merchantGpsLat: 5.414100, merchantGpsLng: 100.328700 },
      { agentId: 'a0000000-0000-0000-0000-000000000005', agentCode: 'AGT-005', businessName: 'Nurul Huda Mart', phoneNumber: '011-6543210', status: 'ACTIVE', tier: 'STANDARD', merchantGpsLat: 1.492700, merchantGpsLng: 103.743100 },
      { agentId: 'a0000000-0000-0000-0000-000000000006', agentCode: 'AGT-006', businessName: 'Tan Kah Seng', phoneNumber: '017-1234567', status: 'ACTIVE', tier: 'BASIC', merchantGpsLat: 4.597500, merchantGpsLng: 101.092100 },
      { agentId: 'a0000000-0000-0000-0000-000000000007', agentCode: 'AGT-007', businessName: 'Ali Ahmad', phoneNumber: '019-1111111', status: 'INACTIVE', tier: 'BASIC', merchantGpsLat: 3.1, merchantGpsLng: 101.6 },
    ]),
    createAgent: vi.fn().mockResolvedValue({}),
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

describe('Agents', () => {
  it('should render agents page', async () => {
    renderWithQuery(<Agents />)
    
    await waitFor(() => {
      expect(screen.getByText('Agent Management')).toBeInTheDocument()
    })
  })

  it('should render Add Agent button', async () => {
    renderWithQuery(<Agents />)
    
    await waitFor(() => {
      expect(screen.getByTestId('add-agent-button')).toBeInTheDocument()
    })
  })

  it('should open Add Agent modal when button is clicked', async () => {
    renderWithQuery(<Agents />)
    
    await waitFor(() => {
      const addButton = screen.getByTestId('add-agent-button')
      fireEvent.click(addButton)
    })
    
    await waitFor(() => {
      expect(screen.getByTestId('add-agent-modal')).toBeInTheDocument()
      expect(screen.getByText('Add New Agent')).toBeInTheDocument()
    })
  })

  it('should close modal when close button is clicked', async () => {
    renderWithQuery(<Agents />)
    
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
    renderWithQuery(<Agents />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-1-button')).toBeInTheDocument()
      expect(screen.getByTestId('page-2-button')).toBeInTheDocument()
      expect(screen.getByTestId('page-3-button')).toBeInTheDocument()
      expect(screen.getByTestId('prev-page-button')).toBeInTheDocument()
      expect(screen.getByTestId('next-page-button')).toBeInTheDocument()
    })
  })

  it('should change page when page number is clicked', async () => {
    renderWithQuery(<Agents />)
    
    await waitFor(() => {
      expect(screen.getByTestId('page-1-button')).toBeInTheDocument()
    })
    
    fireEvent.click(screen.getByTestId('page-2-button'))
    
    await waitFor(() => {
      expect(screen.getByTestId('page-2-button')).toHaveClass('btn-primary')
    })
  })

  it('should disable prev button on first page', async () => {
    renderWithQuery(<Agents />)
    
    await waitFor(() => {
      const prevButton = screen.getByTestId('prev-page-button')
      expect(prevButton).toBeDisabled()
    })
  })

  it('should enable prev button after navigating to next page', async () => {
    renderWithQuery(<Agents />)
    
    await waitFor(() => {
      fireEvent.click(screen.getByTestId('next-page-button'))
    })
    
    await waitFor(() => {
      const prevButton = screen.getByTestId('prev-page-button')
      expect(prevButton).not.toBeDisabled()
    })
  })
})
