import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { 
  Search, 
  Eye, 
  ChevronLeft, 
  ChevronRight, 
  X,
  LayoutGrid,
  List,
  Calendar
} from 'lucide-react'
import api from '../api/client'
import toast from 'react-hot-toast'
import { WorkflowCard } from '../components/WorkflowCard'
import { WorkflowDetailsModal } from '../components/ActivityTimelineModal'

interface WorkflowItem {
  workflowId: string
  transactionId: string
  transactionType: string
  amount: number | null
  status: string
  createdAt: string
  pendingReason?: string
  agentId?: string
}

export function OrchestratorWorkflows() {
  const queryClient = useQueryClient()
  const [currentPage, setCurrentPage] = useState(0)
  const [searchTerm, setSearchTerm] = useState('')
  const [dateRange, setDateRange] = useState('7')
  const [statusFilter, setStatusFilter] = useState('')
  const [transactionTypeFilter, setTransactionTypeFilter] = useState('')
  const [viewMode, setViewMode] = useState<'table' | 'card'>('card')
  const [selectedWorkflow, setSelectedWorkflow] = useState<WorkflowItem | null>(null)
  const [showDetailModal, setShowDetailModal] = useState(false)
  const [createCaseWorkflowId, setCreateCaseWorkflowId] = useState<string | null>(null);
  const [showCreateCaseModal, setShowCreateCaseModal] = useState(false);
  const pageSize = 20

  const createCaseMutation = useMutation({
    mutationFn: (workflowId: string) => api.createResolutionCase(workflowId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workflows'] })
      queryClient.invalidateQueries({ queryKey: ['resolutions'] })
      toast.success('Resolution case created successfully!')
      setCreateCaseWorkflowId(null)
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.error?.message || 'Failed to create resolution case')
      setCreateCaseWorkflowId(null)
    }
  })

  const getDateRange = () => {
    const now = new Date()
    const from = new Date()
    if (dateRange === '7') from.setDate(now.getDate() - 7)
    else if (dateRange === '30') from.setMonth(now.getMonth() - 1)
    else if (dateRange === '90') from.setMonth(now.getMonth() - 3)
    return {
      fromDate: from.toISOString(),
      toDate: now.toISOString()
    }
  }

  const { data: workflowsResponse, isLoading } = useQuery({
    queryKey: ['workflows', currentPage, dateRange, statusFilter, transactionTypeFilter],
    queryFn: async () => {
      const dates = getDateRange()
      const params = {
        fromDate: dates.fromDate,
        toDate: dates.toDate,
        status: statusFilter || undefined,
        transactionType: transactionTypeFilter || undefined,
        page: currentPage,
        size: pageSize
      }
      const response = await api.getWorkflows(params)
      return response as { content: WorkflowItem[] }
    },
    retry: 1
  })

  const workflows = workflowsResponse?.content || []

  const { data: resolutionsResponse } = useQuery({
    queryKey: ['resolutions'],
    queryFn: async () => {
      const response = await api.getResolutions()
      return response as { content: { workflowId: string; caseId: string; status: string }[] }
    },
    retry: 1
  })

  const resolutionCaseMap = new Map(
    (resolutionsResponse?.content || []).map(c => [c.workflowId, c])
  )

  const extractUuidFromWorkflowId = (workflowId: string): string => {
    if (!workflowId) return ''
    if (workflowId.match(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/)) {
      return workflowId
    }
    const parts = workflowId.split('-')
    if (parts.length > 5) {
      return parts.slice(-5).join('-')
    }
    return workflowId
  }

  const getResolutionCase = (workflowId: string) => {
    const case_ = resolutionCaseMap.get(workflowId)
    if (case_) return case_
    const uuid = extractUuidFromWorkflowId(workflowId)
    return resolutionCaseMap.get(uuid)
  }

  const filteredWorkflows = workflows.filter(w => {
    if (!searchTerm) return true
    return w.workflowId.toLowerCase().includes(searchTerm.toLowerCase()) ||
           w.transactionId.toLowerCase().includes(searchTerm.toLowerCase())
  })

  const handlePageChange = (page: number) => {
    setCurrentPage(page)
  }

  const handleViewDetail = async (workflow: WorkflowItem) => {
    setSelectedWorkflow(workflow)
    setShowDetailModal(true)
  }

  const getStatusBadge = (status: string) => {
    const styles: Record<string, { bg: string; color: string; label: string }> = {
      'COMPLETED': { bg: '#d1fae5', color: '#059669', label: 'Completed' },
      'FAILED': { bg: '#fee2e2', color: '#dc2626', label: 'Failed' },
      'PENDING': { bg: '#fef3c7', color: '#d97706', label: 'Pending' },
      'COMPENSATING': { bg: '#dbeafe', color: '#2563eb', label: 'Compensating' },
      'PENDING_REVIEW': { bg: '#f3e8ff', color: '#9333ea', label: 'Pending Review' },
    }
    const s = styles[status] || styles['PENDING']
    return (
      <span style={{ padding: '4px 10px', borderRadius: 4, fontSize: 12, fontWeight: 600, background: s.bg, color: s.color }}>
        {s.label}
      </span>
    )
  }

  const getTransactionTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      'CASH_WITHDRAWAL': 'Cash Withdrawal',
      'RETAIL_SALE': 'Retail Sale',
      'PREPAID_TOPUP': 'Prepaid Topup',
      'BILL_PAYMENT': 'Bill Payment',
      'DUIT_NOW_TRANSFER': 'DuitNow Transfer',
      'DEPOSIT': 'Deposit'
    }
    return labels[type] || type
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24, padding: 24 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h1 style={{ fontSize: 24, fontWeight: 700, margin: 0, color: '#1e293b' }}>
          Transaction Workflows
        </h1>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 12 }}>
          <div style={{ position: 'relative', flex: 1, maxWidth: 320 }}>
            <Search size={18} color="#64748b" style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)' }} />
            <input
              type="text"
              className="input"
              placeholder="Search by Workflow ID or Transaction ID..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{ paddingLeft: 40 }}
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Calendar size={18} color="#64748b" />
            <select
              className="input"
              style={{ width: 150 }}
              value={dateRange}
              onChange={(e) => { setDateRange(e.target.value); setCurrentPage(0); }}
            >
              <option value="7">Last 7 days</option>
              <option value="30">Last 30 days</option>
              <option value="90">Last 3 months</option>
            </select>
          </div>

          <select
            className="input"
            style={{ width: 160 }}
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setCurrentPage(0); }}
          >
            <option value="">All Status</option>
            <option value="PENDING">Pending</option>
            <option value="COMPLETED">Completed</option>
            <option value="FAILED">Failed</option>
            <option value="COMPENSATING">Compensating</option>
            <option value="PENDING_REVIEW">Pending Review</option>
          </select>

          <select
            className="input"
            style={{ width: 160 }}
            value={transactionTypeFilter}
            onChange={(e) => { setTransactionTypeFilter(e.target.value); setCurrentPage(0); }}
          >
            <option value="">All Types</option>
            <option value="CASH_WITHDRAWAL">Cash Withdrawal</option>
            <option value="RETAIL_SALE">Retail Sale</option>
            <option value="PREPAID_TOPUP">Prepaid Topup</option>
            <option value="BILL_PAYMENT">Bill Payment</option>
            <option value="DUIT_NOW_TRANSFER">DuitNow Transfer</option>
            <option value="DEPOSIT">Deposit</option>
          </select>

          <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginLeft: 'auto', background: '#f1f5f9', padding: 4, borderRadius: 8 }}>
            <button
              onClick={() => setViewMode('table')}
              className={`btn btn-sm ${viewMode === 'table' ? 'btn-primary' : 'btn-ghost'}`}
              style={{ padding: '6px 12px', display: 'flex', alignItems: 'center', gap: 6 }}
              title="Table View"
            >
              <List size={16} />
              Table
            </button>
            <button
              onClick={() => setViewMode('card')}
              className={`btn btn-sm ${viewMode === 'card' ? 'btn-primary' : 'btn-ghost'}`}
              style={{ padding: '6px 12px', display: 'flex', alignItems: 'center', gap: 6 }}
              title="Card View"
            >
              <LayoutGrid size={16} />
              Card
            </button>
          </div>
        </div>
      </div>

      {viewMode === 'card' ? (
        <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
          {isLoading ? (
            <div style={{ textAlign: 'center', padding: 40 }}>Loading...</div>
          ) : filteredWorkflows.length === 0 ? (
            <div style={{ textAlign: 'center', padding: 40, color: '#64748b' }}>
              No workflows found
            </div>
          ) : (
            filteredWorkflows.map((workflow) => (
              <WorkflowCard
                key={workflow.workflowId}
                workflow={workflow}
                resolutionCase={getResolutionCase(workflow.workflowId)}
                onViewDetails={handleViewDetail}
                onCreateCase={(wf) => {
                  setCreateCaseWorkflowId(wf.workflowId)
                  setShowCreateCaseModal(true)
                }}
              />
            ))
          )}
        </div>
      ) : (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Workflow ID</th>
                <th>Transaction ID</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Case Status</th>
                <th>Created At</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr><td colSpan={8} style={{ textAlign: 'center', padding: 40 }}>Loading...</td></tr>
              ) : filteredWorkflows.length === 0 ? (
                <tr><td colSpan={8} style={{ textAlign: 'center', padding: 40, color: '#64748b' }}>No workflows found</td></tr>
              ) : (
                filteredWorkflows.map((workflow) => (
                  <tr key={workflow.workflowId}>
                    <td><code style={{ fontSize: 12 }}>{workflow.workflowId}</code></td>
                    <td><code style={{ fontSize: 12 }}>{workflow.transactionId}</code></td>
                    <td>{getTransactionTypeLabel(workflow.transactionType)}</td>
                    <td>MYR {workflow.amount?.toFixed(2) || '0.00'}</td>
                    <td>{getStatusBadge(workflow.status)}</td>
                    <td>
                      {(() => {
                        const case_ = getResolutionCase(workflow.workflowId)
                        if (!case_) return <span style={{ color: '#94a3b8', fontSize: 12 }}>No case</span>
                        const caseStatusStyles: Record<string, { bg: string; color: string; label: string }> = {
                          'PENDING_MAKER': { bg: '#fef3c7', color: '#d97706', label: 'Pending Maker' },
                          'PENDING_CHECKER': { bg: '#dbeafe', color: '#2563eb', label: 'Pending Checker' },
                          'APPROVED': { bg: '#d1fae5', color: '#059669', label: 'Approved' },
                          'REJECTED': { bg: '#fee2e2', color: '#dc2626', label: 'Rejected' },
                        }
                        const s = caseStatusStyles[case_.status] || caseStatusStyles['PENDING_MAKER']
                        return (
                          <span style={{ padding: '4px 10px', borderRadius: 4, fontSize: 11, fontWeight: 600, background: s.bg, color: s.color }}>
                            {s.label}
                          </span>
                        )
                      })()}
                    </td>
                    <td>{new Date(workflow.createdAt).toLocaleString()}</td>
                    <td>
                      <div style={{ display: 'flex', gap: 8 }}>
                        <button 
                          className="btn btn-outline btn-sm" 
                          onClick={() => handleViewDetail(workflow)}
                          data-testid={`view-${workflow.workflowId}-button`}
                        >
                          <Eye size={14} />
                          View
                        </button>
                        {(() => {
                          const case_ = getResolutionCase(workflow.workflowId)
                          if (case_) {
                            return (
                              <button 
                                className="btn btn-secondary btn-sm" 
                                disabled
                                style={{ opacity: 0.6 }}
                              >
                                <Eye size={14} />
                                View Case
                              </button>
                            )
                          }
                          return (
                            <button 
                              className="btn btn-primary btn-sm"
                              onClick={() => {
                                setShowCreateCaseModal(true);
                                setCreateCaseWorkflowId(workflow.workflowId);
                              }}
                              disabled={createCaseMutation.isPending}
                            >
                              {createCaseMutation.isPending && createCaseWorkflowId === workflow.workflowId ? 'Creating...' : 'Create Case'}
                            </button>
                          )
                        })()}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      <div style={{ 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'space-between',
        padding: 16,
        background: 'white',
        borderRadius: 12,
        border: '1px solid #e2e8f0'
      }}>
        <p style={{ fontSize: 13, color: '#64748b' }}>
          Showing {filteredWorkflows.length} workflows
        </p>
        <div style={{ display: 'flex', gap: 8 }}>
          <button
            className="btn btn-outline btn-sm"
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 0}
          >
            <ChevronLeft size={16} />
            Previous
          </button>
          <button
            className="btn btn-outline btn-sm"
            onClick={() => handlePageChange(currentPage + 1)}
            disabled={filteredWorkflows.length < pageSize}
          >
            Next
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

      {showDetailModal && selectedWorkflow && (
        <WorkflowDetailsModal
          workflow={selectedWorkflow}
          isOpen={showDetailModal}
          onClose={() => { setShowDetailModal(false); setSelectedWorkflow(null); }}
          onCreateCase={() => {
            setCreateCaseWorkflowId(selectedWorkflow.workflowId)
            setShowDetailModal(false)
            setShowCreateCaseModal(true)
          }}
          hasExistingCase={!!getResolutionCase(selectedWorkflow.workflowId)}
          resolutionCase={getResolutionCase(selectedWorkflow.workflowId)}
        />
      )}
      
      {showCreateCaseModal && selectedWorkflow && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.6)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          backdropFilter: 'blur(4px)'
        }} onClick={() => setShowCreateCaseModal(false)}>
          <div style={{
            width: 420,
            background: 'white',
            borderRadius: 16,
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)'
          }} onClick={e => e.stopPropagation()}>
            <div style={{
              padding: 24,
              borderBottom: '1px solid #e2e8f0',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}>
              <h3 style={{ fontSize: 18, fontWeight: 600, margin: 0 }}>Create Resolution Case</h3>
              <button onClick={() => setShowCreateCaseModal(false)} style={{ background: '#f1f5f9', border: 'none', cursor: 'pointer', padding: 8, borderRadius: 8 }}>
                <X size={20} color="#64748b" />
              </button>
            </div>
            <div style={{ padding: 24 }}>
              <p style={{ fontSize: 14, color: '#475569', marginBottom: 16 }}>
                Create a resolution case for the following workflow?
              </p>
              <div style={{ background: '#f8fafc', padding: 16, borderRadius: 12, marginBottom: 20 }}>
                <div style={{ marginBottom: 8 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase' }}>Workflow ID</label>
                  <p style={{ fontSize: 13, fontFamily: 'monospace', margin: '4px 0 0 0' }}>{selectedWorkflow.workflowId}</p>
                </div>
                <div style={{ marginBottom: 8 }}>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase' }}>Transaction Type</label>
                  <p style={{ fontSize: 14, margin: '4px 0 0 0' }}>{getTransactionTypeLabel(selectedWorkflow.transactionType)}</p>
                </div>
                <div>
                  <label style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase' }}>Amount</label>
                  <p style={{ fontSize: 14, margin: '4px 0 0 0' }}>MYR {selectedWorkflow.amount?.toFixed(2) || '0.00'}</p>
                </div>
              </div>
              <div style={{ display: 'flex', gap: 12 }}>
                <button
                  className="btn btn-outline"
                  onClick={() => setShowCreateCaseModal(false)}
                  style={{ flex: 1 }}
                >
                  Cancel
                </button>
                <button
                  className="btn btn-primary"
                  onClick={() => {
                    setCreateCaseWorkflowId(selectedWorkflow.workflowId);
                    createCaseMutation.mutate(selectedWorkflow.workflowId);
                    setShowCreateCaseModal(false);
                  }}
                  disabled={createCaseMutation.isPending}
                  style={{ flex: 1 }}
                >
                  {createCaseMutation.isPending ? 'Creating...' : 'Create Case'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
