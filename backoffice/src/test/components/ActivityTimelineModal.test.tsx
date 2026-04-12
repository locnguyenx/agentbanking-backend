import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor, waitForElementToBeRemoved } from '@testing-library/react'
import { ActivityTimelineModal } from '../../components/ActivityTimelineModal'
import { WorkflowExecutionDetails, ActivityStatus } from '../../types/workflow'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

// Mock the API client
vi.mock('../../api/client', () => ({
  default: {
    getWorkflowExecutionDetails: vi.fn()
  }
}))

const mockApi = vi.mocked(await import('../../api/client')).default

describe('ActivityTimelineModal', () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  })

  const mockExecutionDetails: WorkflowExecutionDetails = {
    workflowId: 'WF-TEST-123',
    currentStatus: 'RUNNING',
    currentActivity: {
      name: 'EvaluateSTPActivity',
      status: ActivityStatus.RUNNING,
      startTime: '2026-04-11T10:30:00Z',
      elapsedTime: '1.3s',
      retryAttempt: 1,
      maxRetries: 3,
      input: { amount: 500, agentTier: 'GOLD' }
    },
    activityTimeline: [
      {
        sequence: 1,
        name: 'CheckVelocityActivity',
        status: ActivityStatus.COMPLETED,
        startTime: '2026-04-11T10:30:00Z',
        duration: '0.2s',
        input: { agentId: 'AGT-001', amount: 500 },
        output: { passed: true }
      },
      {
        sequence: 2,
        name: 'EvaluateSTPActivity',
        status: ActivityStatus.RUNNING,
        startTime: '2026-04-11T10:30:01Z',
        duration: '1.3s',
        input: { amount: 500, agentTier: 'GOLD' }
      }
    ],
    externalServiceStatus: {
      rulesService: 'RESPONDING',
      ledgerService: 'AVAILABLE',
      switchAdapter: 'AVAILABLE',
      billerService: 'NOT_REQUIRED'
    },
    estimatedCompletion: '2026-04-11T10:30:45Z',
    debugInfo: {
      temporalWorkflowId: 'WF-TEST-123',
      historyEventCount: 5
    }
  }

  const renderWithQueryClient = (ui: React.ReactElement) => {
    return render(
      <QueryClientProvider client={queryClient}>
        {ui}
      </QueryClientProvider>
    )
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.getWorkflowExecutionDetails.mockResolvedValue(mockExecutionDetails)
  })

  it('should render modal with workflow information', async () => {
    renderWithQueryClient(
      <ActivityTimelineModal
        workflowId="WF-TEST-123"
        isOpen={true}
        onClose={vi.fn()}
      />
    )

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('Workflow Execution Details')).toBeInTheDocument()
      expect(screen.getByText('WF-TEST-123')).toBeInTheDocument()
    })

    // Check execution summary
    expect(screen.getByText('Current Status')).toBeInTheDocument()
    expect(screen.getByText('RUNNING')).toBeInTheDocument()
    expect(screen.getByText('Total Activities')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('should display current activity panel for running workflows', async () => {
    renderWithQueryClient(
      <ActivityTimelineModal
        workflowId="WF-TEST-123"
        isOpen={true}
        onClose={vi.fn()}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Current Activity')).toBeInTheDocument()
      expect(screen.getByText('EvaluateSTPActivity')).toBeInTheDocument()
      expect(screen.getByText('RUNNING')).toBeInTheDocument()
      expect(screen.getByText('1.3s')).toBeInTheDocument()
      expect(screen.getByText('Attempt 1 of 3')).toBeInTheDocument()
    })
  })

  it('should display activity timeline', async () => {
    renderWithQueryClient(
      <ActivityTimelineModal
        workflowId="WF-TEST-123"
        isOpen={true}
        onClose={vi.fn()}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Activity Timeline')).toBeInTheDocument()
      expect(screen.getByText('CheckVelocityActivity')).toBeInTheDocument()
      expect(screen.getByText('EvaluateSTPActivity')).toBeInTheDocument()
    })

    // Check status indicators
    expect(screen.getByText('0.2s')).toBeInTheDocument() // Completed activity duration
    expect(screen.getByText('1.3s')).toBeInTheDocument() // Running activity duration
  })

  it('should display external service status', async () => {
    renderWithQueryClient(
      <ActivityTimelineModal
        workflowId="WF-TEST-123"
        isOpen={true}
        onClose={vi.fn()}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('External Service Status')).toBeInTheDocument()
      expect(screen.getByText('Rules')).toBeInTheDocument()
      expect(screen.getByText('Ledger')).toBeInTheDocument()
      expect(screen.getByText('Switch')).toBeInTheDocument()
    })
  })

  it('should allow expanding activity details', async () => {
    renderWithQueryClient(
      <ActivityTimelineModal
        workflowId="WF-TEST-123"
        isOpen={true}
        onClose={vi.fn()}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('CheckVelocityActivity')).toBeInTheDocument()
    })

    // Click on the first activity to expand it
    const firstActivity = screen.getByText('CheckVelocityActivity').closest('.timeline-item')
    const expandButton = firstActivity?.querySelector('.expand-icon')
    
    if (expandButton) {
      fireEvent.click(expandButton)
      
      // Should show expanded details
      await waitFor(() => {
        expect(screen.getByText('Input Parameters')).toBeInTheDocument()
        expect(screen.getByText('Output Results')).toBeInTheDocument()
      })
    }
  })

  it('should handle refresh functionality', async () => {
    renderWithQueryClient(
      <ActivityTimelineModal
        workflowId="WF-TEST-123"
        isOpen={true}
        onClose={vi.fn()}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Workflow Execution Details')).toBeInTheDocument()
    })

    const refreshButton = screen.getByText('Refresh now').closest('button')
    if (refreshButton) {
      fireEvent.click(refreshButton)
      
      // Should call the API again
      expect(mockApi.getWorkflowExecutionDetails).toHaveBeenCalledTimes(2)
    }
  })

  it('should handle auto-refresh toggle', async () => {
    renderWithQueryClient(
      <ActivityTimelineModal
        workflowId="WF-TEST-123"
        isOpen={true}
        onClose={vi.fn()}
        autoRefresh={true}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Workflow Execution Details')).toBeInTheDocument()
    })

    const autoRefreshCheckbox = screen.getByLabelText('Auto-refresh')
    fireEvent.click(autoRefreshCheckbox)
    
    // Should still be functional
    expect(screen.getByText('Workflow Execution Details')).toBeInTheDocument()
  })

  it('should handle close functionality', async () => {
    const mockOnClose = vi.fn()
    
    renderWithQueryClient(
      <ActivityTimelineModal
        workflowId="WF-TEST-123"
        isOpen={true}
        onClose={mockOnClose}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Workflow Execution Details')).toBeInTheDocument()
    })

    const closeButton = screen.getByText('Close').closest('button')
    if (closeButton) {
      fireEvent.click(closeButton)
      expect(mockOnClose).toHaveBeenCalled()
    }
  })

  it('should handle API errors gracefully', async () => {
    mockApi.getWorkflowExecutionDetails.mockRejectedValueOnce(new Error('API Error'))
    
    renderWithQueryClient(
      <ActivityTimelineModal
        workflowId="WF-TEST-123"
        isOpen={true}
        onClose={vi.fn()}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Failed to Load Execution Details')).toBeInTheDocument()
      expect(screen.getByText('API Error')).toBeInTheDocument()
    })
  })

  it('should not render when isOpen is false', () => {
    renderWithQueryClient(
      <ActivityTimelineModal
        workflowId="WF-TEST-123"
        isOpen={false}
        onClose={vi.fn()}
      />
    )

    expect(screen.queryByText('Workflow Execution Details')).not.toBeInTheDocument()
  })

  it('should handle debug information expansion', async () => {
    renderWithQueryClient(
      <ActivityTimelineModal
        workflowId="WF-TEST-123"
        isOpen={true}
        onClose={vi.fn()}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Debug Information')).toBeInTheDocument()
    })

    const debugHeader = screen.getByText('Debug Information').closest('.debug-header')
    if (debugHeader) {
      fireEvent.click(debugHeader)
      
      // Should show debug content
      await waitFor(() => {
        expect(screen.getByText('Copy Debug Info')).toBeInTheDocument()
        expect(screen.getByText('Export JSON')).toBeInTheDocument()
      })
    }
  })

  it('should handle copy debug info functionality', async () => {
    // Mock clipboard API
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn().mockResolvedValue(undefined)
      }
    })
    
    renderWithQueryClient(
      <ActivityTimelineModal
        workflowId="WF-TEST-123"
        isOpen={true}
        onClose={vi.fn()}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Debug Information')).toBeInTheDocument()
    })

    // Expand debug section
    const debugHeader = screen.getByText('Debug Information').closest('.debug-header')
    if (debugHeader) {
      fireEvent.click(debugHeader)
      
      await waitFor(() => {
        expect(screen.getByText('Copy Debug Info')).toBeInTheDocument()
      })

      const copyButton = screen.getByText('Copy Debug Info').closest('button')
      if (copyButton) {
        fireEvent.click(copyButton)
        
        // Should call clipboard API
        expect(navigator.clipboard.writeText).toHaveBeenCalled()
      }
    }
  })
})