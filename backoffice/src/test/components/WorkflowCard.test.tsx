import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { WorkflowCard } from '../../components/WorkflowCard'
import { EnhancedWorkflowItem, ActivityStatus } from '../../types/workflow'

// Mock the ActivityTimelineModal
vi.mock('../../components/ActivityTimelineModal', () => ({
  ActivityTimelineModal: ({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) => (
    isOpen ? <div data-testid="activity-modal">Activity Timeline Modal</div> : null
  )
}))

describe('WorkflowCard', () => {
  const mockWorkflow: EnhancedWorkflowItem = {
    workflowId: 'WF-TEST-123',
    transactionId: 'TX-TEST-456',
    transactionType: 'CASH_WITHDRAWAL',
    amount: 500.00,
    status: 'PENDING',
    createdAt: '2026-04-11T10:30:00Z',
    pendingReason: 'Awaiting switch response',
    agentId: 'AGT-001',
    customerFee: 2.50,
    activities: [
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
    currentActivity: {
      name: 'EvaluateSTPActivity',
      status: ActivityStatus.RUNNING,
      startTime: '2026-04-11T10:30:01Z',
      elapsedTime: '1.3s',
      retryAttempt: 1,
      maxRetries: 3,
      input: { amount: 500, agentTier: 'GOLD' }
    },
    elapsedTime: '1.5s',
    externalServiceStatus: {
      rulesService: 'RESPONDING',
      ledgerService: 'AVAILABLE',
      switchAdapter: 'AVAILABLE',
      billerService: 'NOT_REQUIRED'
    }
  }

  const mockOnViewDetails = vi.fn()
  const mockOnCreateCase = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render workflow card with basic information', () => {
    render(
      <WorkflowCard 
        workflow={mockWorkflow}
        onViewDetails={mockOnViewDetails}
        onCreateCase={mockOnCreateCase}
      />
    )

    // Check basic workflow information
    expect(screen.getByText('Cash Withdrawal')).toBeInTheDocument()
    expect(screen.getByText('WF-TEST-123')).toBeInTheDocument()
    expect(screen.getByText('TX-TEST-456')).toBeInTheDocument()
    expect(screen.getByText('MYR 500.00')).toBeInTheDocument()
    expect(screen.getByText('Fee: MYR 2.50')).toBeInTheDocument()
    
    // Check status badge
    expect(screen.getByText('Pending')).toBeInTheDocument()
    
    // Check elapsed time
    expect(screen.getByText('1.5s')).toBeInTheDocument()
  })

  it('should display current activity information for pending workflows', () => {
    render(
      <WorkflowCard 
        workflow={mockWorkflow}
        onViewDetails={mockOnViewDetails}
        onCreateCase={mockOnCreateCase}
      />
    )

    // Check current activity is displayed
    expect(screen.getByText('EvaluateSTPActivity')).toBeInTheDocument()
    expect(screen.getByText('RUNNING')).toBeInTheDocument()
    expect(screen.getByText('1.3s')).toBeInTheDocument()
  })

  it('should display pending reason for pending workflows', () => {
    render(
      <WorkflowCard 
        workflow={mockWorkflow}
        onViewDetails={mockOnViewDetails}
        onCreateCase={mockOnCreateCase}
      />
    )

    // Check pending reason is displayed
    expect(screen.getByText('Awaiting switch response')).toBeInTheDocument()
  })

  it('should display service status indicators', () => {
    render(
      <WorkflowCard 
        workflow={mockWorkflow}
        onViewDetails={mockOnViewDetails}
        onCreateCase={mockOnCreateCase}
      />
    )

    // Check service status indicators are present
    expect(screen.getByText('Rules')).toBeInTheDocument()
    expect(screen.getByText('Ledger')).toBeInTheDocument()
    expect(screen.getByText('Switch')).toBeInTheDocument()
  })

  it('should call onViewDetails when View Details button is clicked', () => {
    render(
      <WorkflowCard 
        workflow={mockWorkflow}
        onViewDetails={mockOnViewDetails}
        onCreateCase={mockOnCreateCase}
      />
    )

    const viewDetailsButton = screen.getByTestId('view-WF-TEST-123-details')
    fireEvent.click(viewDetailsButton)

    expect(mockOnViewDetails).toHaveBeenCalledWith('WF-TEST-123')
  })

  it('should call onCreateCase when Create Case button is clicked for pending workflows', () => {
    render(
      <WorkflowCard 
        workflow={mockWorkflow}
        onViewDetails={mockOnViewDetails}
        onCreateCase={mockOnCreateCase}
      />
    )

    const createCaseButton = screen.getByTestId('create-case-WF-TEST-123')
    fireEvent.click(createCaseButton)

    expect(mockOnCreateCase).toHaveBeenCalledWith('WF-TEST-123')
  })

  it('should not show Create Case button for completed workflows', () => {
    const completedWorkflow = { ...mockWorkflow, status: 'COMPLETED' }
    
    render(
      <WorkflowCard 
        workflow={completedWorkflow}
        onViewDetails={mockOnViewDetails}
        onCreateCase={mockOnCreateCase}
      />
    )

    expect(screen.queryByTestId('create-case-WF-TEST-123')).not.toBeInTheDocument()
  })

  it('should handle workflows without activities gracefully', () => {
    const workflowWithoutActivities = { ...mockWorkflow, activities: undefined, currentActivity: undefined }
    
    render(
      <WorkflowCard 
        workflow={workflowWithoutActivities}
        onViewDetails={mockOnViewDetails}
        onCreateCase={mockOnCreateCase}
      />
    )

    // Should still render basic information
    expect(screen.getByText('Cash Withdrawal')).toBeInTheDocument()
    expect(screen.getByText('WF-TEST-123')).toBeInTheDocument()
    
    // Should not show current activity section
    expect(screen.queryByText('EvaluateSTPActivity')).not.toBeInTheDocument()
  })

  it('should handle different transaction types with correct icons', () => {
    const transactionTypes = [
      { type: 'CASH_WITHDRAWAL', expected: 'Cash Withdrawal' },
      { type: 'RETAIL_SALE', expected: 'Retail Sale' },
      { type: 'PREPAID_TOPUP', expected: 'Prepaid Topup' },
      { type: 'BILL_PAYMENT', expected: 'Bill Payment' },
      { type: 'DUIT_NOW_TRANSFER', expected: 'DuitNow Transfer' },
      { type: 'DEPOSIT', expected: 'Deposit' }
    ]

    transactionTypes.forEach(({ type, expected }) => {
      const workflow = { ...mockWorkflow, transactionType: type }
      
      const { unmount } = render(
        <WorkflowCard 
          workflow={workflow}
          onViewDetails={mockOnViewDetails}
          onCreateCase={mockOnCreateCase}
        />
      )

      expect(screen.getByText(expected)).toBeInTheDocument()
      unmount()
    })
  })

  it('should calculate workflow progress correctly', () => {
    const workflowWithProgress = { 
      ...mockWorkflow, 
      activities: [
        { sequence: 1, name: 'Activity1', status: ActivityStatus.COMPLETED },
        { sequence: 2, name: 'Activity2', status: ActivityStatus.RUNNING },
        { sequence: 3, name: 'Activity3', status: ActivityStatus.SCHEDULED }
      ]
    }
    
    render(
      <WorkflowCard 
        workflow={workflowWithProgress}
        onViewDetails={mockOnViewDetails}
        onCreateCase={mockOnCreateCase}
      />
    )

    // Should show progress text
    expect(screen.getByText('1 of 3 steps completed')).toBeInTheDocument()
    expect(screen.getByText('Currently: Activity2')).toBeInTheDocument()
  })
})