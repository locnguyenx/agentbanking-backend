import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { TransactionResolution } from '../pages/TransactionResolution'
import React from 'react'
import '@testing-library/jest-dom'

vi.mock('../api/client', () => {
  const MOCK_API_ITEM = {
    caseId: 'case-123',
    workflowId: 'wf-123',
    transactionId: 'tx-123',
    agentId: 'agent-123',
    amount: 500.0,
    transactionType: 'CASH_DEPOSIT',
    status: 'PENDING_MAKER',
    customerFee: null,
    referenceNumber: null,
    proposedAction: '',
    reasonCode: '',
    reason: '',
    makerUserId: '',
    makerCreatedAt: '2026-03-25T14:30:00Z',
    checkerUserId: '',
    checkerAction: '',
    checkerReason: '',
    checkerCompletedAt: '',
    errorCode: '',
    errorMessage: '',
    completedAt: '',
    createdAt: '2026-03-25T14:30:00Z',
    updatedAt: '2026-03-25T14:30:00Z',
    makerPendingReason: 'AWAITING_SWITCH_RESPONSE',
    checkerPendingReason: ''
  }

  const MOCK_RESOLUTIONS_RESPONSE = {
    content: [MOCK_API_ITEM],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 10
  }

  const MOCK_API = {
    getResolutions: vi.fn().mockResolvedValue(MOCK_RESOLUTIONS_RESPONSE),
    getResolution: vi.fn().mockResolvedValue(MOCK_API_ITEM),
    proposeResolution: vi.fn(),
    approveResolution: vi.fn(),
    rejectResolution: vi.fn(),
    forceResolve: vi.fn()
  }

  return {
    default: MOCK_API,
    api: MOCK_API
  }
})

function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: Infinity },
    },
  })
}

describe('TransactionResolution', () => {
  it('should render resolution page and show defaults in drawer', async () => {
    const queryClient = createTestQueryClient()
    
    render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          <TransactionResolution />
        </QueryClientProvider>
      </MemoryRouter>
    )
    
    const viewButton = await screen.findByTestId('view-wf-123-button', {}, { timeout: 4000 })
    fireEvent.click(viewButton)

    const drawerTitle = await screen.findByTestId('resolution-details-title', {}, { timeout: 4000 })
    expect(drawerTitle).toBeTruthy()

    expect(screen.getByText(/Customer Fee/i)).toBeTruthy()
    expect(screen.getByText(/MYR -/i)).toBeTruthy()
    expect(screen.getByText(/MYR 500\.00/i)).toBeTruthy()
    expect(screen.getByText(/Reference Number/i)).toBeTruthy()
  })
})
