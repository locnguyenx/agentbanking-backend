import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { OrchestratorWorkflows } from '../pages/OrchestratorWorkflows'
import React from 'react'

vi.mock('../api/client', () => {
  const MOCK_WORKFLOWS_RESPONSE = {
    content: [
      {
        workflowId: 'wf-123',
        transactionId: 'tx-123',
        status: 'COMPLETED',
        transactionType: 'CASH_DEPOSIT',
        amount: 100.0,
        createdAt: '2026-03-25T14:30:00Z',
        agentId: 'agent-1',
        pendingReason: ''
      }
    ],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 20
  }

  const MOCK_STATUS_RESPONSE = {
    status: 'COMPLETED',
    pendingReason: null,
    workflowId: 'wf-123',
    transactionId: 'tx-123',
    amount: 100.0,
    customerFee: null,
    referenceNumber: null,
    completedAt: '2026-03-25T14:35:00Z'
  }

  const MOCK_API = {
    getWorkflows: vi.fn().mockResolvedValue(MOCK_WORKFLOWS_RESPONSE),
    getWorkflowStatus: vi.fn().mockResolvedValue(MOCK_STATUS_RESPONSE),
    getResolutions: vi.fn().mockResolvedValue({ content: [] }),
    getPartialResolutions: vi.fn().mockResolvedValue({ content: [] }),
    createResolutionCase: vi.fn().mockResolvedValue({})
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

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = createTestQueryClient()
  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
    </MemoryRouter>
  )
}

describe('OrchestratorWorkflows', () => {
  it('should render workflows page and open details with defaults', async () => {
    renderWithProviders(<OrchestratorWorkflows />)
    
    // Wait for the page title to be visible
    await waitFor(() => {
      expect(screen.getByText(/Orchestrator Workflows/i)).toBeTruthy()
    })

    // Find and click the View button
    const viewButton = await screen.findByTestId('view-wf-123-button')
    fireEvent.click(viewButton)

    // Check modal content
    await waitFor(() => {
      expect(screen.getByTestId('workflow-details-title')).toBeTruthy()
      expect(screen.getByText(/MYR -/i)).toBeTruthy()
    }, { timeout: 4000 })
  })
})
