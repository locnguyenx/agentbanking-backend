import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { AuditLogTable } from '../components/AuditLogTable'
import React from 'react'

const mockLogs = [
  {
    auditId: 'a0000000-0000-0000-0000-000000000001',
    serviceName: 'auth-iam-service',
    entityType: 'USER',
    action: 'USER_CREATED',
    performedBy: 'admin-001',
    ipAddress: '192.168.1.1',
    timestamp: '2026-04-05T10:30:00',
    outcome: 'SUCCESS',
    failureReason: null,
  },
  {
    auditId: 'a0000000-0000-0000-0000-000000000002',
    serviceName: 'onboarding-service',
    entityType: 'AGENT',
    action: 'AGENT_CREATED',
    performedBy: 'admin-001',
    ipAddress: '192.168.1.1',
    timestamp: '2026-04-05T10:31:00',
    outcome: 'FAILURE',
    failureReason: 'Duplicate agent ID',
  },
]

describe('AuditLogTable', () => {
  it('should render audit logs with all columns', () => {
    render(<AuditLogTable logs={mockLogs} />)
    expect(screen.getByText('USER_CREATED')).toBeInTheDocument()
    expect(screen.getByText('SUCCESS')).toBeInTheDocument()
    expect(screen.getByText('AGENT_CREATED')).toBeInTheDocument()
  })

  it('should show FAILURE outcome with error styling', () => {
    render(<AuditLogTable logs={mockLogs} />)
    expect(screen.getByText('FAILURE')).toHaveClass('badge-error')
  })

  it('should show failure reason for failed operations', async () => {
    render(<AuditLogTable logs={mockLogs} />)
    const viewButton = screen.getAllByText('View')[1]
    fireEvent.click(viewButton)
    await waitFor(() => {
      expect(screen.getByText('Audit Log Details')).toBeInTheDocument()
    })
    expect(screen.getByText('Duplicate agent ID')).toBeInTheDocument()
  })

  it('should show empty state when no logs', () => {
    render(<AuditLogTable logs={[]} />)
    expect(screen.getByText(/No audit logs found/)).toBeInTheDocument()
  })

  it('should show loading state', () => {
    render(<AuditLogTable logs={[]} isLoading={true} />)
    expect(screen.getByText(/Loading/)).toBeInTheDocument()
  })

  it('should render pagination when provided', () => {
    render(
      <AuditLogTable
        logs={mockLogs}
        pagination={{ page: 0, size: 20, totalElements: 100, totalPages: 5 }}
      />
    )
    expect(screen.getByText(/Page 1 of 5/)).toBeInTheDocument()
  })

  it('should call onPageChange when next button clicked', () => {
    const mockPageChange = vi.fn()
    render(
      <AuditLogTable
        logs={mockLogs}
        pagination={{ page: 0, size: 20, totalElements: 100, totalPages: 5 }}
        onPageChange={mockPageChange}
      />
    )
    screen.getByText('Next').click()
    expect(mockPageChange).toHaveBeenCalledWith(1)
  })

  it('should disable previous button on first page', () => {
    render(
      <AuditLogTable
        logs={mockLogs}
        pagination={{ page: 0, size: 20, totalElements: 100, totalPages: 5 }}
      />
    )
    expect(screen.getByText('Previous')).toBeDisabled()
  })

  it('should disable next button on last page', () => {
    render(
      <AuditLogTable
        logs={mockLogs}
        pagination={{ page: 4, size: 20, totalElements: 100, totalPages: 5 }}
      />
    )
    expect(screen.getByText('Next')).toBeDisabled()
  })
})
